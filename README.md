# InReq - IntelliJ Plugin

Request from inside. Lightweight API client for IntelliJ — type your endpoint, hit Send, debug your code.

Skip switching to Postman or managing heavy collections. Test your APIs directly where you write your code.

## Setup (one time)

1. Install: Settings > Plugins > gear > Install Plugin from Disk > select `inreq-plugin-1.0.0.zip`
2. Restart IntelliJ
3. Set your port: Settings > Tools > InReq > Server port (default: 8080)

## Usage

1. Start your application in IntelliJ (Run or Debug, both work)
2. Open the InReq panel (right side tab)
3. Type your endpoint: `v1/policy/createOrSave`
4. Hit Send (or press Enter, or Alt+Shift+R)
5. If running in debug mode, your breakpoints fire automatically

## Features

- One Send button, works in both Run and Debug mode
- Ctrl+F search in request body and response body
- Request tabs: Headers, Auth, Body, Params (with add/remove buttons)
- Request section collapses after Send for full response space
- Click any request tab to expand it back
- Request history in the History tab, click to replay
- Pretty-printed JSON responses
- Configurable port, timeout, and SSL verification

## Settings

Settings > Tools > InReq

| Setting     | Default | What it does                                        |
|-------------|---------|-----------------------------------------------------|
| Server port | 8080    | The port your application runs on (e.g. 8080, 8290) |
| Timeout     | 30s     | HTTP request timeout                                |
| Verify SSL  | Off     | Turn on for HTTPS endpoints                         |

## Keyboard shortcuts

| Shortcut               | Action         |
|------------------------|----------------|
| Enter (in URL field)   | Send request   |
| Alt+Shift+R            | Send request   |
| Ctrl+F (Cmd+F on Mac)  | Search in body |
| Escape                 | Close search   |

## For your team

Share the ZIP: `inreq-plugin-1.0.0.zip`

Each developer:
1. Settings > Plugins > gear > Install Plugin from Disk
2. Settings > Tools > InReq > set their server port
3. Start their app, open the panel, start sending requests

No external dependencies. No cloud services. Everything runs locally.

