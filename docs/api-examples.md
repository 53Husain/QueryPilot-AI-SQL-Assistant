# QueryPilot API examples

Import [the Postman collection](api.postman_collection.json) after completing
[local setup](local-setup.md). Compose initializes synthetic employees Asha
(Engineering, 65000) and Rohan (Support, 42000) on its first startup.

- HTTP startup check returns a plain-text running message. It does not call the AI.
- Ask about demo employees requires your own provider key. It sends the question
  and discovered schema to the provider and may incur a charge.
- Inspect `sql`, `data`, `rowCount` and `message`; HTTP 200 also covers some rejections.
  The model can choose different SQL, so the collection does not claim an exact
  model output. A correct salary > 50000 query returns Asha from the bundled data.

CI starts this application with a local mock of the provider's chat-completions
endpoint. It verifies that schema discovery includes employees, executes a fixed
SELECT against the Compose database and checks the returned row. A separate
check proves that the application database account cannot INSERT. This validates
local wiring and permissions, **not live provider access or model accuracy**.
The Postman request itself uses whichever provider you configure; it does not
start a mock automatically. No real API key is included in the repository.
