const statsNode = document.getElementById("stats");
const rowsNode = document.getElementById("prediction-rows");
const lastUpdateNode = document.getElementById("last-update");
const regionControlsNode = document.getElementById("region-controls");
const feedTitleNode = document.getElementById("feed-title");
const regionSummaryNode = document.getElementById("region-summary");

const REGIONS = [
    {
        key: "global",
        label: "Global",
        title: "Global wildfire focus",
        bounds: {west: -140, east: 55, south: -40, north: 65}
    },
    {
        key: "europe",
        label: "Europe",
        title: "Europe wildfire focus",
        bounds: {west: -12, east: 35, south: 34, north: 60},
        excludedCountries: ["Algeria", "Morocco", "Tunisia", "Kenya/Tanzania", "South Africa", "United States"]
    },
    {
        key: "america",
        label: "America",
        title: "America wildfire focus",
        bounds: {west: -130, east: -85, south: 14, north: 58}
    },
    {
        key: "africa",
        label: "Africa",
        title: "Africa wildfire focus",
        bounds: {west: -20, east: 55, south: -36, north: 38},
        excludedCountries: ["Spain", "Portugal", "France", "Italy", "Greece", "Romania", "Bulgaria", "United States"]
    }
];

const state = {
    regionKey: "global",
    snapshot: null
};

function riskClass(riskLevel) {
    return String(riskLevel || "").toLowerCase();
}

function formatDate(value) {
    if (!value) {
        return "No updates yet";
    }
    return new Date(value).toLocaleString();
}

function formatNumber(value) {
    return Number.isFinite(value) ? value.toFixed(1) : "0.0";
}

function statCard(label, value) {
    return `
        <article class="stat-card">
            <div class="label">${label}</div>
            <div class="value">${value}</div>
        </article>
    `;
}

function currentRegion() {
    return REGIONS.find((region) => region.key === state.regionKey) || REGIONS[0];
}

function isWithinBounds(prediction, bounds) {
    return prediction.latitude >= bounds.south
        && prediction.latitude <= bounds.north
        && prediction.longitude >= bounds.west
        && prediction.longitude <= bounds.east;
}

function matchesRegion(prediction, region) {
    if (!isWithinBounds(prediction, region.bounds)) {
        return false;
    }

    if (region.excludedCountries?.includes(prediction.country)) {
        return false;
    }

    return true;
}

function filteredPredictions() {
    const predictions = state.snapshot?.predictions || [];
    const region = currentRegion();
    return predictions.filter((prediction) => matchesRegion(prediction, region));
}

function summaryFor(predictions) {
    const averageThreatScore = predictions.length
        ? predictions.reduce((sum, prediction) => sum + prediction.threatScore, 0) / predictions.length
        : 0;
    const topPrediction = predictions.reduce((top, prediction) => {
        if (!top || prediction.threatScore > top.threatScore) {
            return prediction;
        }
        return top;
    }, null);

    return {
        totalPredictions: predictions.length,
        averageThreatScore,
        extremeRiskCount: predictions.filter((prediction) => prediction.riskLevel === "EXTREME").length,
        highRiskCount: predictions.filter((prediction) => prediction.riskLevel === "HIGH").length,
        topZoneName: topPrediction?.zoneName || "No data",
        topThreatScore: topPrediction?.threatScore ?? 0,
        topPrediction
    };
}

function renderStats(predictions) {
    const region = currentRegion();
    const summary = summaryFor(predictions);
    statsNode.innerHTML = [
        statCard("Focus region", region.label),
        statCard("Visible predictions", summary.totalPredictions),
        statCard("Average threat", formatNumber(summary.averageThreatScore)),
        statCard("Extreme risk", summary.extremeRiskCount),
        statCard("High risk", summary.highRiskCount),
        statCard("Top zone", summary.topZoneName),
        statCard("Top score", formatNumber(summary.topThreatScore))
    ].join("");
}

function renderRows(predictions) {
    if (!predictions.length) {
        rowsNode.innerHTML = `<tr><td colspan="9" class="empty">No predictions for this region yet.</td></tr>`;
        return;
    }

    rowsNode.innerHTML = predictions.slice(0, 25).map((prediction) => `
        <tr>
            <td><span class="badge ${riskClass(prediction.riskLevel)}">${prediction.riskLevel}</span></td>
            <td class="threat">${formatNumber(prediction.threatScore)}</td>
            <td>${prediction.zoneName}</td>
            <td>${prediction.country}</td>
            <td>${prediction.cellId}</td>
            <td>${formatNumber(prediction.predictedSpreadBearing)}&deg;</td>
            <td>${formatNumber(prediction.estimatedSpreadVelocityKph)} km/h</td>
            <td>${prediction.weatherFallbackUsed ? "Yes" : "No"}</td>
            <td>${formatDate(prediction.predictionTime)}</td>
        </tr>
    `).join("");
}

function renderRegionControls() {
    regionControlsNode.innerHTML = REGIONS.map((region) => `
        <button class="region-pill ${region.key === state.regionKey ? "active" : ""}" type="button" data-region="${region.key}">
            ${region.label}
        </button>
    `).join("");
}

function renderHotspotList(predictions) {
    const region = currentRegion();
    feedTitleNode.textContent = `Latest predictions in ${region.label}`;
    regionSummaryNode.textContent = predictions.length
        ? `${predictions.length} prediction${predictions.length === 1 ? "" : "s"} in ${region.label} · Lon ${region.bounds.west} to ${region.bounds.east} · Lat ${region.bounds.south} to ${region.bounds.north}`
        : `No active predictions in ${region.label} · Lon ${region.bounds.west} to ${region.bounds.east} · Lat ${region.bounds.south} to ${region.bounds.north}`;
}

function render() {
    const predictions = filteredPredictions();
    renderRegionControls();
    renderHotspotList(predictions);
    renderStats(predictions);
    renderRows(predictions);

    const lastUpdate = state.snapshot?.lastUpdate;
    lastUpdateNode.textContent = lastUpdate
        ? `Last dashboard update: ${formatDate(lastUpdate)}`
        : "Waiting for data";
}

async function refresh() {
    try {
        const response = await fetch("/api/predictions");
        if (!response.ok) {
            throw new Error(`Prediction API returned ${response.status}`);
        }
        state.snapshot = await response.json();
        render();
    } catch (error) {
        state.snapshot = state.snapshot || {predictions: [], lastUpdate: null};
        render();
        lastUpdateNode.textContent = "Dashboard API unavailable";
    }
}

regionControlsNode.addEventListener("click", (event) => {
    const regionKey = event.target.dataset.region;
    if (!regionKey || regionKey === state.regionKey) {
        return;
    }
    state.regionKey = regionKey;
    render();
});

window.addEventListener("resize", () => {
    render();
});

async function init() {
    renderRegionControls();
    await refresh();
    setInterval(refresh, 5000);
}

init();
