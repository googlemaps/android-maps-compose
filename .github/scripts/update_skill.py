# Copyright 2026 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import os
import json
import urllib.request
import sys

def get_gemini_response(api_key, prompt):
    url = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key={api_key}"
    headers = {'Content-Type': 'application/json'}
    data = {
        "contents": [{
            "parts": [{"text": prompt}]
        }]
    }
    
    req = urllib.request.Request(url, data=json.dumps(data).encode('utf-8'), headers=headers)
    try:
        with urllib.request.urlopen(req) as response:
            res_data = json.loads(response.read().decode('utf-8'))
            return res_data['candidates'][0]['content']['parts'][0]['text']
    except urllib.error.HTTPError as e:
        print(f"Gemini API Error ({e.code}): {e.reason}", file=sys.stderr)
        try:
            error_body = e.read().decode('utf-8')
            print(f"Error details: {error_body}", file=sys.stderr)
        except:
            pass
        return None
    except Exception as e:
        print(f"Error calling Gemini API: {e}", file=sys.stderr)
        return None

def main():
    api_key = os.getenv("GEMINI_API_KEY")
    diff_file = os.getenv("DIFF_FILE", "release_diff.patch")
    skill_file = os.getenv("SKILL_FILE", ".gemini/skills/android-maps-compose/SKILL.md")

    if not api_key:
        print("GEMINI_API_KEY not found", file=sys.stderr)
        sys.exit(1)

    if not os.path.exists(diff_file):
        print(f"Diff file {diff_file} not found.", file=sys.stderr)
        sys.exit(1)

    if not os.path.exists(skill_file):
        print(f"Skill file {skill_file} not found.", file=sys.stderr)
        sys.exit(1)

    with open(diff_file, "r") as f:
        diff_content = f.read()

    with open(skill_file, "r") as f:
        skill_content = f.read()

    prompt = f"""
You are an expert technical writer and Android developer. 
Your task is to update the Gemini CLI skill instructions for an SDK based on the latest release changes.

Here is the current `SKILL.md` file:
```markdown
{skill_content}
```

Here is the git diff representing the changes introduced in this release:
```diff
{diff_content}
```

Please analyze the diff to identify any new APIs, deprecated functions, structural changes, or changes in implementation best practices.
Update the `SKILL.md` content to incorporate these new concepts or deprecations.

CRITICAL REQUIREMENTS:
- Preserve the overall markdown structure and formatting of the existing `SKILL.md`.
- Ensure you keep the YAML frontmatter intact at the top of the file (between `---`).
- Do NOT remove the `// x-release-please-version` comments in the Gradle dependencies, as they are required by our release process.
- Return ONLY the raw updated markdown content. Do NOT wrap it in ```markdown...``` code blocks, just return the exact file content so it can be directly saved.
"""

    print("Requesting update from Gemini...", file=sys.stderr)
    response_text = get_gemini_response(api_key, prompt)
    if response_text:
        # Clean up response text if the model wrapped it in markdown code blocks despite instructions
        if response_text.startswith("```markdown"):
            response_text = response_text.replace("```markdown\n", "", 1)
            if response_text.endswith("```"):
                response_text = response_text[:-3]
        elif response_text.startswith("```"):
            response_text = response_text.replace("```\n", "", 1)
            if response_text.endswith("```"):
                response_text = response_text[:-3]
                
        # Trim whitespace at the very beginning or end
        response_text = response_text.strip() + "\n"
            
        with open(skill_file, "w") as f:
            f.write(response_text)
        print(f"Successfully updated {skill_file}", file=sys.stderr)
    else:
        sys.exit(1)

if __name__ == "__main__":
    main()
