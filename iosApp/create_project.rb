require 'xcodeproj'

# Initialize project
project_path = 'iosApp.xcodeproj'
project = Xcodeproj::Project.new(project_path)

# Set deployment target to iOS 16.0 project-wide
project.build_configurations.each do |config|
  config.build_settings['IPHONEOS_DEPLOYMENT_TARGET'] = '16.0'
end

# Create group for source files (maps to the iosApp directory)
group = project.main_group.new_group('iosApp', 'iosApp')

# Reference source files inside the group
app_delegate_ref = group.new_file('AppDelegate.swift')
sample_list_ref = group.new_file('SampleListViewController.swift')
secrets_ref = group.new_file('DeveloperSecrets.swift')
info_plist_ref = group.new_file('Info.plist')

# Create target
target = project.new_target(:application, 'iosApp', :ios, '16.0')

# Configure target settings
target.build_configurations.each do |config|
  config.build_settings['PRODUCT_BUNDLE_IDENTIFIER'] = 'com.google.maps.android.compose.iosApp'
  config.build_settings['INFOPLIST_FILE'] = 'iosApp/Info.plist'
  config.build_settings['SWIFT_VERSION'] = '5.0'
  config.build_settings['SDKROOT'] = 'iphoneos'
  config.build_settings['TARGETED_DEVICE_FAMILY'] = '1,2'
  config.build_settings['CODE_SIGNING_REQUIRED'] = 'NO'
  config.build_settings['CODE_SIGNING_ALLOWED'] = 'NO'
  config.build_settings['AD_HOC_CODE_SIGNING_ALLOWED'] = 'YES'
end

# Add a Build Phase Run Script to populate secrets before compilation
populate_secrets_phase = target.new_shell_script_build_phase('Populate Secrets')
populate_secrets_phase.shell_script = <<-SHELL
SECRETS_PATH="${PROJECT_DIR}/../secrets.properties"
OUTPUT_FILE="${SRCROOT}/iosApp/DeveloperSecrets.swift"

API_KEY="YOUR_API_KEY"

if [ -f "$SECRETS_PATH" ]; then
    EXTRACTED_KEY=$(grep -E "^MAPS_API_KEY=" "$SECRETS_PATH" | cut -d'=' -f2 | tr -d '"' | tr -d "'")
    if [ ! -z "$EXTRACTED_KEY" ]; then
        API_KEY="$EXTRACTED_KEY"
    fi
fi

cat <<EOF > "$OUTPUT_FILE"
// Generated file. Do not commit or modify.
struct DeveloperSecrets {
    static let mapsApiKey = "$API_KEY"
}
EOF
SHELL

# Move the secrets phase to the very beginning of target build phases
target.build_phases.delete(populate_secrets_phase)
target.build_phases.insert(0, populate_secrets_phase)

# Add files to their respective build phases
source_build_phase = target.source_build_phase
source_build_phase.add_file_reference(app_delegate_ref)
source_build_phase.add_file_reference(sample_list_ref)
source_build_phase.add_file_reference(secrets_ref)

project.save
puts "Xcode project created successfully!"

