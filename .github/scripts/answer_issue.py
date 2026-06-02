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
    # Using gemini-1.5-flash for fast and reliable answering
    url = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key={api_key}"
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
    issue_title = os.getenv("ISSUE_TITLE")
    issue_body = os.getenv("ISSUE_BODY")
    skill_file = os.getenv("SKILL_FILE", ".gemini/skills/android-maps-compose/SKILL.md")
    response_file = os.getenv("RESPONSE_FILE", "issue_response.md")

    if not api_key:
        print("GEMINI_API_KEY not found", file=sys.stderr)
        sys.exit(1)
        
    if not issue_title and not issue_body:
        print("Error: ISSUE_TITLE and ISSUE_BODY are both empty. Answering skipped.", file=sys.stderr)
        sys.exit(0)

    # Read skills context if it exists
    skill_content = ""
    if os.path.exists(skill_file):
        with open(skill_file, "r") as f:
            skill_content = f.read()
    else:
        print(f"Warning: Skill file {skill_file} not found. Proceeding without skills context.", file=sys.stderr)

    prompt = f"""
You are an expert AI maintainer for the `android-maps-compose` open-source library.
Your task is to answer a user's GitHub issue in a helpful, friendly, professional, and highly accurate manner.

Here is the repository's `SKILL.md` guide containing the self-updating skills and latest setup instructions:
```markdown
{skill_content}
```

Below are the details of the issue submitted by the user:
- **Title**: {issue_title}
- **Body**: 
{issue_body}

Your response should:
1. Welcome and thank the user for reaching out.
2. Provide a clear, correct explanation or troubleshooting steps.
3. Reference the `SKILL.md` patterns where appropriate (e.g., correct dependencies, secrets plugin setup, or marker states).
4. Provide idiomatic Jetpack Compose and Kotlin code examples if applicable.
5. If the issue is a bug report that needs library maintainers to inspect, let them know, but still provide potential workarounds or troubleshooting steps.
6. Keep your tone humble, polite, and constructive. Do not use overly formal or robotic language.

Please return ONLY the markdown content of your comment to the user. Do not wrap your entire response in a code block.
"""

    print("Requesting issue response from Gemini...", file=sys.stderr)
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
                
        response_text = response_text.strip()
        
        with open(response_file, "w") as f:
            f.write(response_text)
        print(f"Successfully wrote issue response to {response_file}", file=sys.stderr)
    else:
        sys.exit(1)

if __name__ == "__main__":
    main()
