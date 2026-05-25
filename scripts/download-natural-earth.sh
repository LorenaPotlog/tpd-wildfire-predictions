#!/usr/bin/env bash
set -euo pipefail

target_dir="${1:-data/natural-earth}"

mkdir -p "$target_dir/admin0" "$target_dir/admin1"

curl -L "https://naciscdn.org/naturalearth/10m/cultural/ne_10m_admin_0_countries.zip" -o "$target_dir/admin0.zip"
unzip -o "$target_dir/admin0.zip" -d "$target_dir/admin0"

curl -L "https://naciscdn.org/naturalearth/10m/cultural/ne_10m_admin_1_states_provinces.zip" -o "$target_dir/admin1.zip"
unzip -o "$target_dir/admin1.zip" -d "$target_dir/admin1"

echo "Admin 0 shapefile: $target_dir/admin0/ne_10m_admin_0_countries.shp"
echo "Admin 1 shapefile: $target_dir/admin1/ne_10m_admin_1_states_provinces.shp"

