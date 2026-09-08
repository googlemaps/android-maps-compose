# Changelog

## [8.6.0](https://github.com/googlemaps/android-maps-compose/compare/v8.5.0...v8.6.0) (2026-09-03)


### Features

* expose map zoom limits ([#949](https://github.com/googlemaps/android-maps-compose/issues/949)) ([e0d4572](https://github.com/googlemaps/android-maps-compose/commit/e0d4572f8b4291e285fe529b8e500be10696c6d0))
* **maps-compose:** default mapColorScheme to FOLLOW_SYSTEM in GoogleMap ([#986](https://github.com/googlemaps/android-maps-compose/issues/986)) ([5f1d438](https://github.com/googlemaps/android-maps-compose/commit/5f1d4382a8f67cf526f7a9b647f4c9babf0893b8))


### Bug Fixes

* guard against null in StreetView panorama change listeners ([#970](https://github.com/googlemaps/android-maps-compose/issues/970)) ([6147f75](https://github.com/googlemaps/android-maps-compose/commit/6147f75ba5d6e39b4a1fa8ae3b18acb50e2e1189))
* **maps-compose:** apply mapColorScheme to GoogleMapOptions during MapView creation ([#976](https://github.com/googlemaps/android-maps-compose/issues/976)) ([7dec2b7](https://github.com/googlemaps/android-maps-compose/commit/7dec2b7a38260821970160373836bed126458d80))
* pin ViewTreeLifecycleOwner/SavedStateRegistryOwner on MapView to prevent info window crash ([#972](https://github.com/googlemaps/android-maps-compose/issues/972)) ([4024515](https://github.com/googlemaps/android-maps-compose/commit/4024515b0fb3d7efd817104de619f336c6418a36))

## [8.5.0](https://github.com/googlemaps/android-maps-compose/compare/v8.4.0...v8.5.0) (2026-08-17)


### Features

* expose info window customization for advanced markers ([#965](https://github.com/googlemaps/android-maps-compose/issues/965)) ([4c65f8c](https://github.com/googlemaps/android-maps-compose/commit/4c65f8c05017fba36af2286b0114d9c3288802e8))


### Bug Fixes

* avoid re-parenting crash for MarkerInfoWindowContent/MarkerInfoWindowComposable ([#953](https://github.com/googlemaps/android-maps-compose/issues/953)) ([12f35a4](https://github.com/googlemaps/android-maps-compose/commit/12f35a4af2090265042c0bbab12084ce5c114f39))
* avoid zero-size crash in rememberComposeBitmapDescriptor when used inside Clustering ([#963](https://github.com/googlemaps/android-maps-compose/issues/963)) ([11a2430](https://github.com/googlemaps/android-maps-compose/commit/11a2430291d263ad84ff73ca4d82ef2797cc7bd0))
* rename InvalidatingComposeView getRotation param to avoid View shadowing ([#952](https://github.com/googlemaps/android-maps-compose/issues/952)) ([1d971cd](https://github.com/googlemaps/android-maps-compose/commit/1d971cd14fb7f708d246ae2b17836950538f1e56))

## [8.4.0](https://github.com/googlemaps/android-maps-compose/compare/v8.3.1...v8.4.0) (2026-07-16)


### Features

* add rotation support to cluster item API ([#943](https://github.com/googlemaps/android-maps-compose/issues/943)) ([3430231](https://github.com/googlemaps/android-maps-compose/commit/343023171d583a52426cc53513e264e8ccbfa788))
* allow GoogleMap to opt out of keyboard focus traversal ([#945](https://github.com/googlemaps/android-maps-compose/issues/945)) ([7049991](https://github.com/googlemaps/android-maps-compose/commit/7049991755f26dfc251ca5075de3bef93673902c))


### Bug Fixes

* **deps:** update dependencies across android-maps-compose ([d4064c7](https://github.com/googlemaps/android-maps-compose/commit/d4064c7307f2a970ce63f4856073a43cc476e400))
* keep ComposeView attached while info window is shown for compose-ui 1.10+ compat ([#931](https://github.com/googlemaps/android-maps-compose/issues/931)) ([995ff38](https://github.com/googlemaps/android-maps-compose/commit/995ff3822e98df2707e9e5f2b43339d6cee27517))
* prevent ComposeUiClusterRenderer crash on fast back gesture with compose-ui 1.10+ ([#930](https://github.com/googlemaps/android-maps-compose/issues/930)) ([3882afa](https://github.com/googlemaps/android-maps-compose/commit/3882afa582fda77b08c50587b4192f6205309cf0))

## [8.3.1](https://github.com/googlemaps/android-maps-compose/compare/v8.3.0...v8.3.1) (2026-07-07)


### Bug Fixes

* keep maps as single tab focus targets ([#935](https://github.com/googlemaps/android-maps-compose/issues/935)) ([2f5b340](https://github.com/googlemaps/android-maps-compose/commit/2f5b340db673a8fc13826b32aed4dc506e312972))
* toggle map color scheme from system state ([#933](https://github.com/googlemaps/android-maps-compose/issues/933)) ([ec1276c](https://github.com/googlemaps/android-maps-compose/commit/ec1276c44d3ac732ceaa6108540cd669d1360581))

## [8.3.0](https://github.com/googlemaps/android-maps-compose/compare/v8.2.2...v8.3.0) (2026-04-09)


### Features

* add WMS tile overlay support to maps-compose-utils ([#884](https://github.com/googlemaps/android-maps-compose/issues/884)) ([ca66e98](https://github.com/googlemaps/android-maps-compose/commit/ca66e982e70899f6deb487689b8eeac9751a94a2))
* expose rememberComposeBitmapDescriptor as public experimental API ([#867](https://github.com/googlemaps/android-maps-compose/issues/867)) ([3456db0](https://github.com/googlemaps/android-maps-compose/commit/3456db01fba52c66f18a367a6110d5e2c092dea4))


### Bug Fixes

* fallback to moveCamera in lite mode for CameraPositionState animate ([#877](https://github.com/googlemaps/android-maps-compose/issues/877)) ([8994e12](https://github.com/googlemaps/android-maps-compose/commit/8994e123730aeece4faec3b51706a4ec020db7e7))
* prevent NullPointerException in MapUpdater on HMS/microG devices ([#858](https://github.com/googlemaps/android-maps-compose/issues/858)) ([5669abd](https://github.com/googlemaps/android-maps-compose/commit/5669abd84ac20c5764ff0fdd73eef050828a577b))

## [8.2.2](https://github.com/googlemaps/android-maps-compose/compare/v8.2.1...v8.2.2) (2026-03-12)


### Bug Fixes

* avoid ComposeNotIdleException when clusterContent is null ([#855](https://github.com/googlemaps/android-maps-compose/issues/855)) ([3b12a24](https://github.com/googlemaps/android-maps-compose/commit/3b12a24dfb67ffdaaeed553af12368e074529ed9))

## [8.2.1](https://github.com/googlemaps/android-maps-compose/compare/v8.2.0...v8.2.1) (2026-03-12)


### Bug Fixes

* fixed issue with ProGuard/R8 and AttributionId ([#861](https://github.com/googlemaps/android-maps-compose/issues/861)) ([7b9149d](https://github.com/googlemaps/android-maps-compose/commit/7b9149d0fd60ad309e2d820c2b536702fd092314))
* prevent NoSuchElementException when computing view keys in ClusterRenderer ([#857](https://github.com/googlemaps/android-maps-compose/issues/857)) ([e34b50f](https://github.com/googlemaps/android-maps-compose/commit/e34b50ff7e3edcf147ef256069e6bc19d983dc43))

## [8.2.0](https://github.com/googlemaps/android-maps-compose/compare/v8.1.0...v8.2.0) (2026-02-24)


### Features

* added Clustering decoration ([#848](https://github.com/googlemaps/android-maps-compose/issues/848)) ([aa5793a](https://github.com/googlemaps/android-maps-compose/commit/aa5793a920f92c1efb0b90287092a33661acac4c))

## [8.1.0](https://github.com/googlemaps/android-maps-compose/compare/v8.0.1...v8.1.0) (2026-02-06)


### Features

* added anchor and zIndex to Cluster ([#839](https://github.com/googlemaps/android-maps-compose/issues/839)) ([c2f19e4](https://github.com/googlemaps/android-maps-compose/commit/c2f19e45c9dfad7060b47f4b51d43ff5dca326f7))

## [8.0.1](https://github.com/googlemaps/android-maps-compose/compare/v8.0.0...v8.0.1) (2026-01-29)


### Bug Fixes

* GroundOverlay ([#826](https://github.com/googlemaps/android-maps-compose/issues/826)) ([f54d4ea](https://github.com/googlemaps/android-maps-compose/commit/f54d4eaf8f61c5da725dd80898d42fa9adf8e126))

## [8.0.0](https://github.com/googlemaps/android-maps-compose/compare/v7.0.0...v8.0.0) (2026-01-27)


### ⚠ BREAKING CHANGES

* update Maps SDK to v20.0.0 and add internal usage attribution ([#830](https://github.com/googlemaps/android-maps-compose/issues/830))

### Features

* update Maps SDK to v20.0.0 and add internal usage attribution ([#830](https://github.com/googlemaps/android-maps-compose/issues/830)) ([2ba9689](https://github.com/googlemaps/android-maps-compose/commit/2ba9689d2446c566129a63eb1b03d4dbf5135635))

## [7.0.0](https://github.com/googlemaps/android-maps-compose/compare/v6.12.2...v7.0.0) (2025-12-16)


### ⚠ BREAKING CHANGES

* updated Compose version ([#805](https://github.com/googlemaps/android-maps-compose/issues/805))

### Features

* updated Compose version ([#805](https://github.com/googlemaps/android-maps-compose/issues/805)) ([447aad0](https://github.com/googlemaps/android-maps-compose/commit/447aad052df4445fc973c82a785d872efdbcbd49))


### Bug Fixes

* Corrected broken links in Support section of README.md ([#796](https://github.com/googlemaps/android-maps-compose/issues/796)) ([1780317](https://github.com/googlemaps/android-maps-compose/commit/1780317c0da577e706493972be73831f75321f23))
