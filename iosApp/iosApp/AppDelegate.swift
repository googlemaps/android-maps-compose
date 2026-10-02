import UIKit
import GoogleMaps
import maps_compose_multiplatform_demo

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
        // The demos are shared with the Android demo app, see maps-compose-multiplatform-demo.
        window?.rootViewController = MainViewControllerKt.MainViewController()
        window?.makeKeyAndVisible()
        
        return true
    }
}
