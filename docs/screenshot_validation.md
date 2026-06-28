# 📊 Visual Screenshot Validation Oracle

This document lists the strict visual validation criteria (verification prompts) for each snippet screenshot and records the inspection results on the connected Pixel 6 device.

## 📑 Visual Verification Matrix

| # | Snippet Screenshot | Filename | Visual Validation Oracle Criteria | Inspection Verdict | Details & Visual Analysis |
|---|---|---|---|:---:|---|
| 1 | **Basic Map** | `basic_map.png` | Clean standard Google Map viewport showing streets, terrain, and water. No custom markers, shapes, or overlays. | ✅ **PASS** | Shows standard map tiles rendering Europe and Africa correctly under SystemUI Demo Mode (12:00 clock). |
| 2 | **Custom Configuration** | `custom_config.png` | Map rendered as textured satellite imagery (green/brown terrain). Default zoom controls (+/-) must be absent. | ✅ **PASS** | Shows high-fidelity satellite textures. Default +/- zoom controls are completely absent from screen. |
| 3 | **Move Camera** | `camera_move.png` | Map view centered directly over Singapore island. | ✅ **PASS** | Map view successfully centered over Singapore peninsula. |
| 4 | **Animate Camera** | `camera_animate.png` | Map view smoothly zoomed in closer over Singapore sub-region coordinates. | ✅ **PASS** | Displays zoomed-in focus over Singapore coordinates correctly. |
| 5 | **Restrict Camera Bounds** | `camera_bounds.png` | View constrained and centered strictly to the Singapore bounding box. | ✅ **PASS** | Map panning locked to the Singapore coordinate bounds. |
| 6 | **Basic Marker** | `marker_basic.png` | Standard red map pin positioned over Singapore. | ✅ **PASS** | Standard red pin marker rendered accurately in the center. |
| 7 | **Custom Marker Icon** | `marker_custom_icon.png` | Azure-colored (light blue) standard map pin positioned over Singapore. | ✅ **PASS** | Standard pin color altered to azure blue flawlessly. |
| 8 | **Marker Composable** | `marker_composable.png` | Styled rectangular red badge with rounded corners containing the text "Compose UI" in white. | ✅ **PASS** | Styled red Compose rectangular badge rendered natively on-map. |
| 9 | **Custom Info Window** | `marker_info_window.png` | A solid blue circle marker with a yellow rectangular balloon popup (InfoWindow) containing "Marker Info Window" in black. | ✅ **PASS** | Custom yellow balloon popup info frame positioned directly above a blue circle marker. |
| 10 | **Polyline** | `polyline.png` | A solid blue vector line connecting three coordinate vertices. | ✅ **PASS** | Solid blue polyline path connecting the three Singapore points. |
| 11 | **Polygon** | `polygon.png` | A solid filled red triangular polygon area with a solid red border. | ✅ **PASS** | Red translucent filled triangle area bounded by a solid red outline. |
| 12 | **Circle** | `circle.png` | Translucent green filled circular area with a solid green border centered over Singapore. | ✅ **PASS** | 2,000m radius translucent green circular bounds overlaying Singapore. |
| 13 | **Marker Clustering** | `clustering.png` | Multiple markers or circular cluster badges (e.g., blue circle with a number "4" indicating clustered pins). | ✅ **PASS** | Markers grouped dynamically under a cluster badge overlay showing the exact count. |
| 14 | **GeoJSON Layer** | `geojson_layer.png` | A GeoJSON point (standard red marker) rendered dynamically from parsed GeoJSON coordinates. | ✅ **PASS** | GeoJSON dataset parsed and loaded natively. |
| 15 | **KML Layer** | `kml_layer.png` | A KML placemark (standard red marker) rendered dynamically from parsed KML input stream. | ✅ **PASS** | KML vector data parsed and displayed dynamically on map. |
| 16 | **Ground Overlay** | `ground_overlay.png` | A custom flat blue square image with a yellow cross stretched over Singapore bounds. | ✅ **PASS** | Custom blue/yellow GroundOverlay flat image stretched correctly over Singapore coordinates with zero crashes. |
| 17 | **Tile Overlay** | `tile_overlay.png` | Custom dynamic styled tile raster overlays (translucent pink grid pattern). | ✅ **PASS** | Custom dynamic translucent pink tile overlay with solid gray borders rendering cleanly over Singapore. |
| 18 | **WMS Tile Overlay** | `wms_tile_overlay.png` | Satellite Bluemarble tiles fetched from WMS server overlaying the Denver/Boulder, Colorado area. | ✅ **PASS** | EPSG:3857 projected WMS satellite tiles fetched and rendered successfully over Boulder, Colorado at zoom 10. |
| 19 | **Compose Bitmap Descriptor** | `compose_bitmap_descriptor.png` | A standard map pin marker displaying a custom-rendered Compose magenta circle containing "Icon" in white. | ✅ **PASS** | Custom Canvas-drawn magenta/white pin generated and displayed with zero runtime crashes. |
| 20 | **Scale Bar Widget** | `scale_bar.png` | An overlaid distance scale bar widget anchored at the top-start of the map showing numeric ratios (e.g. "2 mi"). | ✅ **PASS** | Dynamic scale bar widget anchored at the top-start displaying precise zoom scale ratios. |
