$env:LLM_API_KEY = "sk-602df32eec054221865dbcd5ca9de45a"
$env:LLM_BASE_URL = "https://api.deepseek.com"
$env:JAVA_TOOL_OPTIONS = "-Dfile.encoding=UTF-8"
cd D:\ai-fms\backend
./mvnw spring-boot:run 2>&1 | Tee-Object -FilePath D:\ai-fms\backend.log
