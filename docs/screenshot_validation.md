# 📊 Visual Screenshot Validation Oracle

This document lists the strict visual validation criteria (verification prompts) for each snippet screenshot and records the inspection results on the connected Pixel 6 device.

## 📑 Visual Verification Matrix

| # | Snippet Screenshot | Filename | Visual Validation Oracle Criteria | Inspection Verdict | Details & Visual Analysis |
|---|---|---|---|:---:|---|
| 1 | **Basic Map** | `basic_map.png` | Clean standard Google Map viewport showing streets, terrain, and water. No custom markers, shapes, or overlays. | ✅ **PASS** | Shows standard map tiles rendering Europe and Africa correctly. Map appbar displays '1. Basic Map' title. |
| 2 | **Custom Configuration** | `custom_config.png` | Map rendered as textured satellite imagery (green/brown terrain). Default zoom controls (+/-) must be absent. | ✅ **PASS** | Shows high-fidelity satellite textures, and default +/- zoom controls are completely absent from screen. |
| 3 | **Move Camera** | `camera_move.png` | Map view centered directly over Singapore island. | **PENDING** | |
| 4 | **Animate Camera** | `camera_animate.png` | Map view smoothly zoomed in closer over Singapore sub-region coordinates. | **PENDING** | |
| 5 | **Restrict Camera Bounds** | `camera_bounds.png` | View constrained and centered strictly to the Singapore bounding box. | **PENDING** | |
| 6 | **Basic Marker** | `marker_basic.png` | Standard red map pin positioned over Singapore. | **PENDING** | |
| 7 | **Custom Marker Icon** | `marker_custom_icon.png` | Azure-colored (light blue) standard map pin positioned over Singapore. | **PENDING** | |
| 8 | **Marker Composable** | `marker_composable.png` | Styled rectangular red badge with rounded corners containing the text "Compose UI" in white. | **PENDING** | |
| 9 | **Custom Info Window** | `marker_info_window.png` | A solid blue circle marker with a yellow rectangular balloon popup (InfoWindow) containing "Marker Info Window" in black. | **PENDING** | |
| 10 | **Polyline** | `polyline.png` | A solid blue vector line connecting three coordinate vertices. | **PENDING** | |
| 11 | **Polygon** | `polygon.png` | A solid filled red triangular polygon area with a solid red border. | **PENDING** | |
| 12 | **Circle** | `circle.png` | Translucent green filled circular area with a solid green border centered over Singapore. | **PENDING** | |
| 13 | **Marker Clustering** | `clustering.png` | Multiple markers or circular cluster badges (e.g., blue circle with a number "4" indicating clustered pins). | **PENDING** | |
| 14 | **GeoJSON Layer** | `geojson_layer.png` | A GeoJSON point (standard red marker) rendered dynamically from parsed GeoJSON coordinates. | **PENDING** | |
| 15 | **KML Layer** | `kml_layer.png` | A KML placemark (standard red marker) rendered dynamically from parsed KML input stream. | **PENDING** | |
| 16 | **Ground Overlay** | `ground_overlay.png` | A flat image (default red pin marker icon) stretched flatly over coordinates in Singapore. | **PENDING** | |
| 17 | **Tile Overlay** | `tile_overlay.png` | Custom dynamic styled tile raster overlays on top of standard view (translucent overlay). | **PENDING** | |
| 18 | **WMS Tile Overlay** | `wms_tile_overlay.png` | Layer tiles fetched from demo WMS server overlaying the map view. | **PENDING** | |
| 19 | **Compose Bitmap Descriptor** | `compose_bitmap_descriptor.png` | A standard map pin marker displaying a custom-rendered Compose magenta circle containing "Icon" in white. | **PENDING** | |
| 20 | **Scale Bar Widget** | `scale_bar.png` | An overlaid distance scale bar widget anchored at the top-start of the map showing numeric ratios (e.g. "10 km"). | **PENDING** | |

