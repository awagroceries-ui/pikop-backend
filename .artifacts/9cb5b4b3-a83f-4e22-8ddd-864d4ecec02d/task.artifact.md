# 📌 Task Checklist: Admin Dashboard & Guest Real-Road Live Tracking Map Upgrade

- `[/]` Task 1: Upgrade Admin Live Tracking View (`admin_track.ejs`)
  - `[ ]` Integrate OSRM real-road GeoJSON route fetcher for pickup and delivery locations
  - `[ ]` Render solid brand green route polyline (`color: '#00E676'`, `weight: 5`, `opacity: 0.95`, `lineCap: 'round'`, `lineJoin: 'round'`)
  - `[ ]` Fetch and render agent active route segment to target destination in solid brand green
  - `[ ]` Smoothly glide agent marker along the real-road polyline on socket `location_updated` events

- `[ ]` Task 2: Upgrade Guest Live Tracking View (`guest_tracking.ejs`)
  - `[ ]` Integrate OSRM real-road GeoJSON route fetcher for delivery tracking
  - `[ ]` Render solid brand green route polyline (`color: '#00E676'`, `weight: 5`, `opacity: 0.95`)
  - `[ ]` Smoothly glide agent marker on socket `location_updated` events

- `[ ]` Task 3: Git Automation & VPS Deployment
  - `[ ]` Stage, commit, and push changes to GitHub `main`
  - `[ ]` Provide VPS deployment command prompts
