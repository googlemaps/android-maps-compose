import UIKit
import GoogleMaps
import maps_compose_multiplatform

@main
class AppDelegate: UIResponder, UIApplicationDelegate {
    var window: UIWindow?

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
    ) -> Bool {
        // Initialize Google Maps SDK dynamically.
        GMSServices.provideAPIKey(DeveloperSecrets.mapsApiKey)

        window = UIWindow(frame: UIScreen.main.bounds)
        let sampleListVC = SampleListViewController()
        let navController = UINavigationController(rootViewController: sampleListVC)
        
        window?.rootViewController = navController
        window?.makeKeyAndVisible()
        
        return true
    }
}
