#!/bin/bash

# Interactive deployment script for LangChain4j modules
set -e

SCRIPT_DIR=$(cd $(dirname $0); pwd)
cd ${SCRIPT_DIR}

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to dynamically discover all langchain4j modules in the project
discover_modules() {
    local modules=()
    
    # Find all directories that:
    # 1. Contain "langchain4j" in their name
    # 2. Have a pom.xml file (indicating they are Maven modules)
    # 3. Are not the target directory or other build artifacts
    while IFS= read -r -d '' module_path; do
        # Get relative path and module name
        local rel_path="${module_path#./}"
        local module_name=$(basename "$module_path")
        
        # Skip if it's a target directory or other build artifacts
        if [[ "$rel_path" == *"/target"* ]] || [[ "$rel_path" == *".git"* ]]; then
            continue
        fi
        
        # Add to modules array
        modules+=("$module_name")
    done < <(find . -type d -name "*langchain4j*" -exec test -f {}/pom.xml \; -print0 | sort -z)
    
    # Remove duplicates and sort
    printf '%s\n' "${modules[@]}" | sort -u
}

# Available modules (dynamically discovered)
MODULES=()

# Repository configurations
get_repo_url() {
    case "$1" in
        "maven.snapshots") echo "http://nexus.coginiti.co:8081/nexus/content/repositories/maven.snapshots" ;;
        "maven.releases") echo "http://nexus.coginiti.co:8081/nexus/content/repositories/maven.releases" ;;
        "maven.ext-releases") echo "http://nexus.coginiti.co:8081/nexus/content/repositories/maven.ext-releases" ;;
        *) echo "" ;;
    esac
}

# Function to print colored output
print_color() {
    printf "${1}%s${NC}\n" "$2"
}

# Function to extract version from POM file
get_pom_version() {
    local pom_file=$1
    if [ ! -f "$pom_file" ]; then
        return 1
    fi
    
    # Try to extract version directly from the POM
    local version=$(grep -m 1 "<version>" "$pom_file" | sed 's/.*<version>\(.*\)<\/version>.*/\1/' | xargs)
    
    # If no version found or it's a property reference, try to get it from parent or root POM
    if [ -z "$version" ] || [[ "$version" == \$\{* ]]; then
        # Check if there's a parent version
        local parent_version=$(grep -A 5 "<parent>" "$pom_file" | grep "<version>" | head -1 | sed 's/.*<version>\(.*\)<\/version>.*/\1/' | xargs)
        if [ -n "$parent_version" ] && [[ "$parent_version" != \$\{* ]]; then
            version="$parent_version"
        else
            # Try to get version from root POM
            local root_pom="./pom.xml"
            if [ -f "$root_pom" ]; then
                version=$(grep -m 1 "<version>" "$root_pom" | sed 's/.*<version>\(.*\)<\/version>.*/\1/' | xargs)
            fi
        fi
    fi
    
    if [ -n "$version" ] && [[ "$version" != \$\{* ]]; then
        echo "$version"
        return 0
    else
        return 1
    fi
}

# Function to detect actual built version from JAR files in target directory
get_built_version() {
    local module_dir=$1
    local artifact_id=$(basename "$module_dir")
    
    if [ ! -d "${module_dir}/target" ]; then
        return 1
    fi
    
    # Look for main JAR file pattern: artifact-id-version.jar
    local jar_file=$(ls "${module_dir}/target/${artifact_id}"-*.jar 2>/dev/null | grep -v sources | grep -v javadoc | grep -v test | head -1)
    
    if [ -n "$jar_file" ]; then
        # Extract version from filename: artifact-version.jar -> version
        local filename=$(basename "$jar_file" .jar)
        local version=${filename#${artifact_id}-}
        echo "$version"
        return 0
    else
        return 1
    fi
}

# Function to find the actual path of a module by name
find_module_path() {
    local module=$1
    
    # Search for the module directory dynamically
    local module_path=$(find . -type d -name "$module" -exec test -f {}/pom.xml \; -print | head -1)
    
    if [ -n "$module_path" ]; then
        # Remove leading './'
        echo "${module_path#./}"
        return 0
    else
        return 1
    fi
}

# Function to check if module exists and has built artifacts or is a POM-only module
check_module() {
    local module=$1
    local module_dir=""
    
    # Use dynamic discovery to find the module path
    module_dir=$(find_module_path "$module")
    if [ $? -ne 0 ]; then
        return 1
    fi
    
    # Check if module directory exists and has either JAR files or is a POM-only module
    if [ ! -d "${module_dir}" ] || [ ! -f "${module_dir}/pom.xml" ]; then
        return 1
    fi
    
    # Check if it's a POM-only module (packaging=pom)
    if grep -q "<packaging>pom</packaging>" "${module_dir}/pom.xml"; then
        echo "${module_dir}"
        return 0
    fi
    
    # For non-POM modules, check if target directory exists and has JAR files
    if [ -d "${module_dir}/target" ] && ls "${module_dir}/target"/*.jar >/dev/null 2>&1; then
        echo "${module_dir}"
        return 0
    else
        return 1
    fi
}

# Function to get JAR file path
get_jar_path() {
    local module_dir=$1
    local version=$2
    local jar_type=$3
    
    if [ "$jar_type" = "main" ]; then
        echo "${module_dir}/target/$(basename $module_dir)-${version}.jar"
    elif [ "$jar_type" = "sources" ]; then
        echo "${module_dir}/target/$(basename $module_dir)-${version}-sources.jar"
    elif [ "$jar_type" = "javadoc" ]; then
        echo "${module_dir}/target/$(basename $module_dir)-${version}-javadoc.jar"
    fi
}

# Function to deploy a single module
deploy_module() {
    local module=$1
    local version=$2
    local repo_id=$3
    local repo_url=$4
    
    print_color $BLUE "Deploying module: $module"
    
    # Check if module exists
    local module_dir=$(check_module "$module")
    if [ $? -ne 0 ]; then
        print_color $RED "Module $module not found or no built artifacts available"
        return 1
    fi
    
    local artifact_id=$(basename "$module_dir")
    local pom_file="${SCRIPT_DIR}/${module_dir}/pom.xml"
    
    # Check if required POM file exists
    if [ ! -f "$pom_file" ]; then
        print_color $RED "POM file not found: $pom_file"
        return 1
    fi
    
    # Check if this is a POM-only module
    local is_pom_only=false
    if grep -q "<packaging>pom</packaging>" "$pom_file"; then
        is_pom_only=true
        print_color $YELLOW "Detected POM-only module: $artifact_id"
    fi
    
    # Build deploy command based on module type
    local deploy_cmd
    if [ "$is_pom_only" = "true" ]; then
        # POM-only module deployment
        deploy_cmd="mvn -N deploy:deploy-file \
            -Durl=$repo_url \
            -DrepositoryId=$repo_id \
            -DgroupId=dev.langchain4j \
            -DartifactId=$artifact_id \
            -Dversion=$version \
            -Dpackaging=pom \
            -Dfile=$pom_file \
            -DpomFile=$pom_file"
    else
        # Regular JAR module deployment
        local main_jar="${SCRIPT_DIR}/$(get_jar_path "$module_dir" "$version" "main")"
        local sources_jar="${SCRIPT_DIR}/$(get_jar_path "$module_dir" "$version" "sources")"
        local javadoc_jar="${SCRIPT_DIR}/$(get_jar_path "$module_dir" "$version" "javadoc")"
        
        # Check if required files exist
        if [ ! -f "$main_jar" ]; then
            print_color $RED "Main JAR not found: $main_jar"
            print_color $YELLOW "Available JARs in target directory:"
            ls -la "${SCRIPT_DIR}/${module_dir}/target"/*.jar 2>/dev/null || print_color $YELLOW "  No JAR files found"
            print_color $YELLOW "This suggests a version mismatch. Try rebuilding the module or use the correct version."
            return 1
        fi
        
        deploy_cmd="mvn -N deploy:deploy-file \
            -Durl=$repo_url \
            -DrepositoryId=$repo_id \
            -DgroupId=dev.langchain4j \
            -DartifactId=$artifact_id \
            -Dversion=$version \
            -Dpackaging=jar \
            -Dfile=$main_jar \
            -DpomFile=$pom_file"
        
        # Add sources if available
        if [ -f "$sources_jar" ]; then
            deploy_cmd="$deploy_cmd -Dsources=$sources_jar"
        fi
        
        # Add javadoc if available
        if [ -f "$javadoc_jar" ]; then
            deploy_cmd="$deploy_cmd -Djavadoc=$javadoc_jar"
        fi
    fi
    
    print_color $YELLOW "Executing: $deploy_cmd"
    
    # Execute deployment from a temporary directory to avoid aggregator POM interference
    TEMP_DIR=$(mktemp -d)
    cd "$TEMP_DIR"
    eval $deploy_cmd
    cd "$SCRIPT_DIR"
    rmdir "$TEMP_DIR"
    
    if [ $? -eq 0 ]; then
        print_color $GREEN "Successfully deployed $artifact_id:$version to $repo_id"
        return 0
    else
        print_color $RED "Failed to deploy $artifact_id:$version"
        return 1
    fi
}

# Main script starts here
print_color $BLUE "=== LangChain4j Module Deployment Script ==="

# Store original arguments for flag checking
ORIGINAL_ARGS=("$@")

# Discover available modules dynamically
print_color $YELLOW "Discovering available langchain4j modules..."
MODULES=()
while IFS= read -r module; do
    MODULES+=("$module")
done < <(discover_modules)
print_color $GREEN "Found ${#MODULES[@]} modules"

echo

# Check if we have command line arguments for non-interactive mode
if [ $# -ge 2 ]; then
    # Non-interactive mode
    SELECTED_MODULES=()
    
    # Check if first argument is "auto" for version auto-detection
    if [ "$1" = "auto" ]; then
        VERSION="auto"
        REPO_ID=$2
        shift 2
    elif [ $# -ge 3 ]; then
        VERSION=$1
        REPO_ID=$2
        shift 2
    else
        # Only repository and modules provided, auto-detect version
        VERSION="auto"
        REPO_ID=$1
        shift 1
    fi
    
    # Rest of arguments are module names (skip --no-build flag)
    while [ $# -gt 0 ]; do
        if [ "$1" != "--no-build" ]; then
            SELECTED_MODULES+=("$1")
        fi
        shift
    done
else
    # Interactive mode
    
    # Get version
    echo
    print_color $YELLOW "Version options:"
    echo "1) Auto-detect from POM files"
    echo "2) Enter version manually"
    echo -n "Select option (1-2): "
    read version_choice
    
    case $version_choice in
        1) 
            VERSION="auto"
            print_color $YELLOW "Will auto-detect version from each module's POM file"
            ;;
        2) 
            echo -n "Enter version (e.g., 1.2.0-SNAPSHOT, 1.2.0-beta8-SNAPSHOT): "
            read VERSION
            if [ -z "$VERSION" ]; then
                print_color $RED "Version cannot be empty"
                exit 1
            fi
            ;;
        *) 
            print_color $RED "Invalid version choice"; 
            exit 1 
            ;;
    esac
    
    # Select repository
    echo
    print_color $YELLOW "Available repositories:"
    echo "1) maven.snapshots"
    echo "2) maven.releases"
    echo "3) maven.ext-releases"
    echo -n "Select repository (1-3): "
    read repo_choice
    
    case $repo_choice in
        1) REPO_ID="maven.snapshots" ;;
        2) REPO_ID="maven.releases" ;;
        3) REPO_ID="maven.ext-releases" ;;
        *) print_color $RED "Invalid repository choice"; exit 1 ;;
    esac
    
    # Select modules
    echo
    print_color $YELLOW "Available modules:"
    for i in "${!MODULES[@]}"; do
        printf "%2d) %s\n" $((i+1)) "${MODULES[$i]}"
    done
    
    echo
    echo "Enter module numbers separated by spaces (e.g., 1 3 5)"
    echo "Or enter 'all' to deploy all available modules"
    echo -n "Selection: "
    read module_selection
    
    SELECTED_MODULES=()
    
    if [ "$module_selection" = "all" ]; then
        SELECTED_MODULES=("${MODULES[@]}")
    else
        for num in $module_selection; do
            if [[ "$num" =~ ^[0-9]+$ ]] && [ "$num" -ge 1 ] && [ "$num" -le "${#MODULES[@]}" ]; then
                SELECTED_MODULES+=("${MODULES[$((num-1))]}")
            else
                print_color $RED "Invalid module number: $num"
                exit 1
            fi
        done
    fi
fi

# Validate repository ID
REPO_URL=$(get_repo_url "$REPO_ID")
if [ -z "$REPO_URL" ]; then
    print_color $RED "Invalid repository ID: $REPO_ID"
    exit 1
fi

# Show deployment summary
echo
print_color $BLUE "=== Deployment Summary ==="
print_color $YELLOW "Version: $VERSION"
print_color $YELLOW "Repository: $REPO_ID ($REPO_URL)"
print_color $YELLOW "Modules to deploy:"
for module in "${SELECTED_MODULES[@]}"; do
    echo "  - $module"
done

echo
echo -n "Proceed with deployment? (y/N): "
read confirm
if [[ ! "$confirm" =~ ^[Yy]$ ]]; then
    print_color $YELLOW "Deployment cancelled"
    exit 0
fi

# Check if --no-build flag is specified
NO_BUILD=false
for arg in "${ORIGINAL_ARGS[@]}"; do
    if [ "$arg" = "--no-build" ]; then
        NO_BUILD=true
        break
    fi
done

# Build and deploy each module sequentially
echo
print_color $BLUE "=== Starting Build and Deployment ==="
SUCCESSFUL_DEPLOYMENTS=0
FAILED_DEPLOYMENTS=0

for module in "${SELECTED_MODULES[@]}"; do
    echo
    print_color $BLUE "=== Processing module: $module ==="
    
    # Build the module first (unless --no-build is specified)
    if [ "$NO_BUILD" = "false" ]; then
        print_color $YELLOW "Building module: $module"
        
        # Find the actual module directory using dynamic discovery
        module_dir=$(find_module_path "$module")
        if [ $? -ne 0 ]; then
            print_color $RED "Module $module not found. Skipping build."
            ((FAILED_DEPLOYMENTS++))
            continue
        fi
        
        # Skip building for POM-only modules
        is_pom_only=false
        if [ -f "${module_dir}/pom.xml" ]; then
            if grep -q "<packaging>pom</packaging>" "${module_dir}/pom.xml"; then
                is_pom_only=true
                print_color $YELLOW "Skipping build for POM-only module: $module"
            fi
        fi
        
        if [ "$is_pom_only" = "false" ]; then
            mvn clean install -DskipTests -Dmaven.test.skip=true -pl "$module_dir" -am
            
            if [ $? -ne 0 ]; then
                print_color $RED "Build failed for module: $module. Skipping deployment."
                ((FAILED_DEPLOYMENTS++))
                continue
            fi
            print_color $GREEN "Successfully built module: $module"
        fi
    fi
    
    # Auto-detect version if needed
    local_version="$VERSION"
    if [ "$VERSION" = "auto" ]; then
        # Get module directory for version detection
        module_dir=$(check_module "$module")
        if [ $? -eq 0 ]; then
            # For regular JAR modules, prefer built version over POM version
            is_pom_only=false
            if grep -q "<packaging>pom</packaging>" "${SCRIPT_DIR}/${module_dir}/pom.xml"; then
                is_pom_only=true
            fi
            
            if [ "$is_pom_only" = "false" ]; then
                # Try to get built version first
                built_version=$(get_built_version "${SCRIPT_DIR}/${module_dir}")
                if [ $? -eq 0 ]; then
                    local_version="$built_version"
                    print_color $YELLOW "Auto-detected built version for $module: $local_version"
                else
                    # Fall back to POM version
                    detected_version=$(get_pom_version "${SCRIPT_DIR}/${module_dir}/pom.xml")
                    if [ $? -eq 0 ]; then
                        local_version="$detected_version"
                        print_color $YELLOW "Auto-detected POM version for $module: $local_version"
                        print_color $YELLOW "Warning: No built artifacts found, using POM version"
                    else
                        print_color $RED "Failed to auto-detect version for $module"
                        ((FAILED_DEPLOYMENTS++))
                        continue
                    fi
                fi
            else
                # For POM-only modules, use POM version
                detected_version=$(get_pom_version "${SCRIPT_DIR}/${module_dir}/pom.xml")
                if [ $? -eq 0 ]; then
                    local_version="$detected_version"
                    print_color $YELLOW "Auto-detected POM version for $module: $local_version"
                else
                    print_color $RED "Failed to auto-detect version for $module"
                    ((FAILED_DEPLOYMENTS++))
                    continue
                fi
            fi
        else
            print_color $RED "Module $module not found for version detection"
            ((FAILED_DEPLOYMENTS++))
            continue
        fi
    fi
    
    # Deploy the module
    if deploy_module "$module" "$local_version" "$REPO_ID" "$REPO_URL"; then
        ((SUCCESSFUL_DEPLOYMENTS++))
        print_color $GREEN "=== Completed processing module: $module ==="
    else
        ((FAILED_DEPLOYMENTS++))
        print_color $RED "=== Failed processing module: $module ==="
    fi
done

# Summary
echo
print_color $BLUE "=== Deployment Complete ==="
print_color $GREEN "Successful deployments: $SUCCESSFUL_DEPLOYMENTS"
if [ $FAILED_DEPLOYMENTS -gt 0 ]; then
    print_color $RED "Failed deployments: $FAILED_DEPLOYMENTS"
fi

if [ $FAILED_DEPLOYMENTS -eq 0 ]; then
    print_color $GREEN "All selected modules deployed successfully!"
    exit 0
else
    print_color $YELLOW "Some deployments failed. Check the output above for details."
    exit 1
fi
