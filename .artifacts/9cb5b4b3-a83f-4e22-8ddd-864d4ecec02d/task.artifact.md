# 📌 Task Checklist: Admin Dashboard & Guest Real-Road Live Tracking Map Upgrade

- `[x]` Task 1: Upgrade Admin Live Tracking View (`admin_track.ejs`)
  - `[x]` Integrate OSRM real-road GeoJSON route fetcher for pickup and delivery locations
  - `[x]` Render solid brand green route polyline (`color: '#00E676'`, `weight: 5`, `opacity: 0.95`, `lineCap: 'round'`, `lineJoin: 'round'`)
  - `[x]` Fetch and render agent active route segment to target destination in solid brand green
  - `[x]` Smoothly glide agent marker along the real-road polyline on socket `location_updated` events

- `[x]` Task 2: Upgrade Guest Live Tracking View (`guest_tracking.ejs`)
  - `[x]` Integrate OSRM real-road GeoJSON route fetcher for delivery tracking
  - `[x]` Render solid brand green route polyline (`color: '#00E676'`, `weight: 5`, `opacity: 0.95`)
  - `[x]` Smoothly glide agent marker on socket `location_updated` events

- `[x]` Task 3: Git Automation & VPS Deployment
  - `[x]` Stage, commit, and push changes to GitHub `main`
  - `[x]` Provide VPS deployment command prompts
