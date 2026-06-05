import UIKit
import GoogleMaps
import maps_compose_multiplatform

class SampleListViewController: UITableViewController {
    
    struct Sample {
        let title: String
        let description: String
        let latitude: Double
        let longitude: Double
        let zoom: Float
        let mapType: MapType
        let myLocationEnabled: Bool
        let scrollGesturesEnabled: Bool
        let zoomGesturesEnabled: Bool
        let markers: [MapMarker]
    }
    
    struct Section {
        let title: String
        let samples: [Sample]
    }
    
    private var sections: [Section] = []
    
    override func viewDidLoad() {
        super.viewDidLoad()
        self.title = "KMP Maps Demos"
        
        setupSections()
        self.tableView.register(UITableViewCell.self, forCellReuseIdentifier: "Cell")
    }
    
    private func setupSections() {
        sections = [
            Section(
                title: "Map Types",
                samples: [
                    Sample(
                        title: "Normal Map",
                        description: "Standard road map of San Francisco",
                        latitude: 37.7749,
                        longitude: -122.4194,
                        zoom: 12.0,
                        mapType: .normal,
                        myLocationEnabled: false,
                        scrollGesturesEnabled: true,
                        zoomGesturesEnabled: true,
                        markers: []
                    ),
                    Sample(
                        title: "Satellite Map",
                        description: "Satellite imagery of the Grand Canyon",
                        latitude: 36.0544,
                        longitude: -112.1401,
                        zoom: 10.0,
                        mapType: .satellite,
                        myLocationEnabled: false,
                        scrollGesturesEnabled: true,
                        zoomGesturesEnabled: true,
                        markers: []
                    ),
                    Sample(
                        title: "Hybrid Map",
                        description: "Satellite with road names in New York City",
                        latitude: 40.7128,
                        longitude: -74.0060,
                        zoom: 11.0,
                        mapType: .hybrid,
                        myLocationEnabled: false,
                        scrollGesturesEnabled: true,
                        zoomGesturesEnabled: true,
                        markers: []
                    ),
                    Sample(
                        title: "Terrain Map",
                        description: "Topographic map of Mount Everest",
                        latitude: 27.9881,
                        longitude: 86.9250,
                        zoom: 10.0,
                        mapType: .terrain,
                        myLocationEnabled: false,
                        scrollGesturesEnabled: true,
                        zoomGesturesEnabled: true,
                        markers: []
                    )
                ]
            ),
            Section(
                title: "Markers & Pins",
                samples: [
                    Sample(
                        title: "Single Marker",
                        description: "Marker at Golden Gate Bridge",
                        latitude: 37.8199,
                        longitude: -122.4783,
                        zoom: 13.0,
                        mapType: .normal,
                        myLocationEnabled: false,
                        scrollGesturesEnabled: true,
                        zoomGesturesEnabled: true,
                        markers: [
                            MapMarker(latitude: 37.8199, longitude: -122.4783, title: "Golden Gate Bridge", snippet: "San Francisco, CA")
                        ]
                    ),
                    Sample(
                        title: "Multiple Markers",
                        description: "Demos with Big Ben, Tower Bridge, London Eye",
                        latitude: 51.5033,
                        longitude: -0.1195,
                        zoom: 13.0,
                        mapType: .normal,
                        myLocationEnabled: false,
                        scrollGesturesEnabled: true,
                        zoomGesturesEnabled: true,
                        markers: [
                            MapMarker(latitude: 51.5007, longitude: -0.1246, title: "Big Ben", snippet: "Historic Clock Tower"),
                            MapMarker(latitude: 51.5055, longitude: -0.0754, title: "Tower Bridge", snippet: "Famous suspension bridge"),
                            MapMarker(latitude: 51.5033, longitude: -0.1195, title: "London Eye", snippet: "Giant Ferris Wheel")
                        ]
                    ),
                    Sample(
                        title: "Custom Snippet Marker",
                        description: "Tokyo Center with descriptive marker information",
                        latitude: 35.6762,
                        longitude: 139.6503,
                        zoom: 11.0,
                        mapType: .normal,
                        myLocationEnabled: false,
                        scrollGesturesEnabled: true,
                        zoomGesturesEnabled: true,
                        markers: [
                            MapMarker(latitude: 35.6762, longitude: 139.6503, title: "Tokyo City", snippet: "Population: 14 million people")
                        ]
                    )
                ]
            ),
            Section(
                title: "Map Gestures & Controls",
                samples: [
                    Sample(
                        title: "Gestures Enabled (Default)",
                        description: "Fully interactive map with zoom and scroll support",
                        latitude: 48.8566,
                        longitude: 2.3522,
                        zoom: 12.0,
                        mapType: .normal,
                        myLocationEnabled: false,
                        scrollGesturesEnabled: true,
                        zoomGesturesEnabled: true,
                        markers: []
                    ),
                    Sample(
                        title: "Gestures Disabled",
                        description: "Static map centered on Rome, Italy (Scroll & zoom locked)",
                        latitude: 41.9028,
                        longitude: 12.4964,
                        zoom: 12.0,
                        mapType: .normal,
                        myLocationEnabled: false,
                        scrollGesturesEnabled: false,
                        zoomGesturesEnabled: false,
                        markers: []
                    ),
                    Sample(
                        title: "Show My Location Button",
                        description: "Request location services & displays Location Button",
                        latitude: 48.8566,
                        longitude: 2.3522,
                        zoom: 12.0,
                        mapType: .normal,
                        myLocationEnabled: true,
                        scrollGesturesEnabled: true,
                        zoomGesturesEnabled: true,
                        markers: []
                    )
                ]
            )
        ]
    }
    
    // MARK: - Table view data source
    
    override func numberOfSections(in tableView: UITableView) -> Int {
        return sections.count
    }
    
    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return sections[section].samples.count
    }
    
    override func tableView(_ tableView: UITableView, titleForHeaderInSection section: Int) -> String? {
        return sections[section].title
    }
    
    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = UITableViewCell(style: .subtitle, reuseIdentifier: "Cell")
        let sample = sections[indexPath.section].samples[indexPath.row]
        cell.textLabel?.text = sample.title
        cell.detailTextLabel?.text = sample.description
        cell.accessoryType = .disclosureIndicator
        return cell
    }
    
    override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        let sample = sections[indexPath.section].samples[indexPath.row]
        
        let mapViewController = GoogleMapKt.GoogleMapViewController(
            latitude: sample.latitude,
            longitude: sample.longitude,
            zoom: sample.zoom,
            mapType: sample.mapType,
            myLocationEnabled: sample.myLocationEnabled,
            scrollGesturesEnabled: sample.scrollGesturesEnabled,
            zoomGesturesEnabled: sample.zoomGesturesEnabled,
            markers: sample.markers
        )
        
        mapViewController.title = sample.title
        self.navigationController?.pushViewController(mapViewController, animated: true)
    }
}
