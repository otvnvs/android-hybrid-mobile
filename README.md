# Android Hybrid Mobile Application

## Building

```bash
./gradlew assemble
```

Or commit with message `release[:title[:description]]` and `./.github/workflows/build.yml` will build a new Release.

# Android Hybrid Mobile Application

## Building

```bash
./gradlew assemble
```

Or commit with message `release[:title[:description]]` and `./.github/workflows/build.yml` will build a new Release.

## Configuration Parameters (`strings.xml`)

The application's runtime boundaries, static asset routing, and secret engineering diagnostic tools are globally controlled via XML metadata. These properties are declared in the layout resource catalog inside:
`app/src/main/res/values/strings.xml`

### Available Keys & Settings Matrix

| Variable Identifier | Expected Data Type | Functional Production Purpose |
| :--- | :---: | :--- |
| `app_name` | `string` | The user-facing software brand title registered natively into the device operating system dashboard layer. |
| `config_workspace_folder_name` | `string` | Sets the physical root subdirectory namespace folder mapped out within the device shared developer workspace directory tracking paths. |
| `virtual_host` | `string` | The base canonical virtual web domain routing alias passed natively down into the embedded view viewport components. |
| `enable_secret_trigger_combination` | `bool` | Flag toggle to activate/deactivate the physical volume hardware combo tracking sequence (`Volume Up` + `Volume Down`) used to load the panel view stack. |

> ** Configuration Fallback Note:** 
> If `config_workspace_folder_name` is left blank, whitespace-only, or omitted entirely from your `strings.xml`, the system automatically defaults to using the unique application package bundle identifier (e.g., `com.example.app`). This guarantees a valid local developer sync directory tree is always securely generated under any environment layout variant.

