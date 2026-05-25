const statsNode = document.getElementById("stats");
const rowsNode = document.getElementById("prediction-rows");
const lastUpdateNode = document.getElementById("last-update");

function riskClass(riskLevel) {
    return String(riskLevel || "").toLowerCase();
}

function formatDate(value) {
    if (!value) {
        return "No updates yet";
    }
    return new Date(value).toLocaleString();
}

function statCard(label, value) {
    return `
        <article class="stat-card">
            <div class="label">${label}</div>
            <div class="value">${value}</div>
        </article>
    `;
}

function renderStats(snapshot) {
    statsNode.innerHTML = [
        statCard("Tracked predictions", snapshot.totalPredictions ?? 0),
        statCard("Average threat", snapshot.averageThreatScore ?? 0),
        statCard("Extreme risk", snapshot.extremeRiskCount ?? 0),
        statCard("High risk", snapshot.highRiskCount ?? 0),
        statCard("Top zone", snapshot.topZoneName || "No data"),
        statCard("Top score", snapshot.topThreatScore ?? 0)
    ].join("");
}

function renderRows(snapshot) {
    const predictions = snapshot.predictions || [];
    if (!predictions.length) {
        rowsNode.innerHTML = `<tr><td colspan="9" class="empty">No predictions yet.</td></tr>`;
        return;
    }

    rowsNode.innerHTML = predictions.slice(0, 25).map((prediction) => `
        <tr>
            <td><span class="badge ${riskClass(prediction.riskLevel)}">${prediction.riskLevel}</span></td>
            <td class="threat">${prediction.threatScore}</td>
            <td>${prediction.zoneName}</td>
            <td>${prediction.country}</td>
            <td>${prediction.cellId}</td>
            <td>${prediction.predictedSpreadBearing}&deg;</td>
            <td>${prediction.estimatedSpreadVelocityKph} km/h</td>
            <td>${prediction.weatherFallbackUsed ? "Yes" : "No"}</td>
            <td>${formatDate(prediction.predictionTime)}</td>
        </tr>
    `).join("");
}

async function refresh() {
    try {
        const response = await fetch("/api/predictions");
        const snapshot = await response.json();
        renderStats(snapshot);
        renderRows(snapshot);
        lastUpdateNode.textContent = snapshot.lastUpdate
            ? `Last dashboard update: ${formatDate(snapshot.lastUpdate)}`
            : "Waiting for data";
    } catch (error) {
        lastUpdateNode.textContent = "Dashboard API unavailable";
    }
}

refresh();
setInterval(refresh, 5000);

