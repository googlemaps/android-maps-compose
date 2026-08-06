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
import glob

def get_gemini_response(api_key, prompt, json_mode=False):
    url = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key={api_key}"
    headers = {'Content-Type': 'application/json'}
    data = {
        "contents": [{
            "parts": [{"text": prompt}]
        }]
    }
    if json_mode:
        data["generationConfig"] = {
            "response_mime_type": "application/json"
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

def list_project_files():
    """Lists relevant source and configuration files in the project."""
    files = []
    # Include Kotlin files, build scripts, Manifests, and properties
    patterns = [
        "**/*.kt",
        "**/*.kts",
        "**/AndroidManifest.xml",
        "**/*.properties",
        "**/*.toml"
    ]
    for pattern in patterns:
        for filepath in glob.glob(pattern, recursive=True):
            # Skip build/bin/gradle-related directories
            if any(part in filepath.split(os.sep) for part in ["build", "bin", ".gradle", ".git", ".antigravitycli"]):
                continue
            files.append(filepath)
    return files

def main():
    api_key = os.getenv("GEMINI_API_KEY")
    issue_title = os.getenv("ISSUE_TITLE")
    issue_body = os.getenv("ISSUE_BODY")
    issue_number = os.getenv("ISSUE_NUMBER", "unknown")

    if not api_key:
        print("GEMINI_API_KEY not found", file=sys.stderr)
        sys.exit(1)
        
    if not issue_title and not issue_body:
        print("Error: ISSUE_TITLE and ISSUE_BODY are empty.", file=sys.stderr)
        sys.exit(1)

    print("Scanning project structure...", file=sys.stderr)
    project_files = list_project_files()
    files_list_str = "\n".join(project_files)

    # 1. Ask Gemini which files need to be modified or created
    selection_prompt = f"""
You are an expert software engineer maintaining the `android-maps-compose` codebase.
A user has submitted the following issue:
- **Title**: {issue_title}
- **Body**: 
{issue_body}

Here is the list of files in the project:
```
{files_list_str}
```

Identify which files need to be modified or created to solve this issue.
Return a JSON object containing:
- `files_to_modify`: A list of file paths from the provided project files list that must be changed.
- `files_to_create`: A list of new file paths that must be created.
- `explanation`: A brief explanation of the proposed changes.

Your response must be valid JSON matching the format:
{{
  "files_to_modify": ["path/to/file1.kt"],
  "files_to_create": ["path/to/newfile.kt"],
  "explanation": "Brief explanation of what needs to be changed."
}}
"""

    print("Determining which files to edit...", file=sys.stderr)
    selection_response = get_gemini_response(api_key, selection_prompt, json_mode=True)
    if not selection_response:
        print("Failed to get file selection from Gemini.", file=sys.stderr)
        sys.exit(1)

    try:
        selection_data = json.loads(selection_response)
    except Exception as e:
        print(f"Failed to parse selection JSON: {e}\nRaw response: {selection_response}", file=sys.stderr)
        sys.exit(1)

    files_to_modify = selection_data.get("files_to_modify", [])
    files_to_create = selection_data.get("files_to_create", [])
    explanation = selection_data.get("explanation", "")

    print(f"Explanation: {explanation}", file=sys.stderr)
    print(f"Modifying: {files_to_modify}", file=sys.stderr)
    print(f"Creating: {files_to_create}", file=sys.stderr)

    # 2. Modify existing files
    for filepath in files_to_modify:
        if not os.path.exists(filepath):
            print(f"File {filepath} not found, skipping modification.", file=sys.stderr)
            continue
        
        with open(filepath, "r") as f:
            original_content = f.read()

        edit_prompt = f"""
You are an expert software engineer. You are fixing the following issue in the `android-maps-compose` codebase:
- **Issue Title**: {issue_title}
- **Issue Body**: {issue_body}

We need to modify the file `{filepath}` to resolve this issue.
Here is the current content of `{filepath}`:
```
{original_content}
```

Please generate the COMPLETE updated content of this file, including all necessary imports and logic, ensuring code style, safety, and correctness.
Return ONLY the updated file contents. Do not wrap the response in any markdown code blocks.
"""

        print(f"Generating changes for {filepath}...", file=sys.stderr)
        updated_content = get_gemini_response(api_key, edit_prompt)
        if updated_content:
            # Clean up wrap
            if updated_content.startswith("```"):
                lines = updated_content.splitlines()
                if lines[0].startswith("```"):
                    lines = lines[1:]
                if lines and lines[-1].strip() == "```":
                    lines = lines[:-1]
                updated_content = "\n".join(lines)
            
            updated_content = updated_content.strip() + "\n"
            
            with open(filepath, "w") as f:
                f.write(updated_content)
            print(f"Successfully updated {filepath}", file=sys.stderr)
        else:
            print(f"Failed to generate changes for {filepath}", file=sys.stderr)
            sys.exit(1)

    # 3. Create new files
    for filepath in files_to_create:
        create_prompt = f"""
You are an expert software engineer. We are resolving this issue:
- **Issue Title**: {issue_title}
- **Issue Body**: {issue_body}

We need to create a new file at `{filepath}`.
Please generate the COMPLETE content of this new file.
Return ONLY the file contents. Do not wrap the response in any markdown code blocks.
"""

        print(f"Generating content for new file {filepath}...", file=sys.stderr)
        new_content = get_gemini_response(api_key, create_prompt)
        if new_content:
            # Clean up wrap
            if new_content.startswith("```"):
                lines = new_content.splitlines()
                if lines[0].startswith("```"):
                    lines = lines[1:]
                if lines and lines[-1].strip() == "```":
                    lines = lines[:-1]
                new_content = "\n".join(lines)
            
            new_content = new_content.strip() + "\n"

            # Create parent directories if they don't exist
            os.makedirs(os.path.dirname(filepath), exist_ok=True)
            with open(filepath, "w") as f:
                f.write(new_content)
            print(f"Successfully created {filepath}", file=sys.stderr)
        else:
            print(f"Failed to generate content for {filepath}", file=sys.stderr)
            sys.exit(1)

    # 4. Generate Branch Name, Commit Message, and Pull Request metadata
    metadata_prompt = f"""
Based on the issue:
- **Title**: {issue_title}
- **Number**: {issue_number}
- **Explanation**: {explanation}

Generate metadata for a Git branch and a Pull Request.
Return a JSON object with:
- `branch_name`: A short, descriptive branch name (e.g. "fix/issue-123-marker-crash").
- `commit_message`: A descriptive commit message following conventional commits (e.g. "fix: resolve crash in marker state calculation").
- `pr_title`: A clear, professional title for the PR.
- `pr_body`: A detailed Markdown description explaining the changes and linking to the issue (e.g. "Fixes #{issue_number}").

Return valid JSON format:
{{
  "branch_name": "fix/...",
  "commit_message": "...",
  "pr_title": "...",
  "pr_body": "..."
}}
"""

    print("Generating Git metadata...", file=sys.stderr)
    metadata_response = get_gemini_response(api_key, metadata_prompt, json_mode=True)
    if metadata_response:
        try:
            metadata_data = json.loads(metadata_response)
            # Write metadata to a file for GitHub Actions to read
            with open("pr_metadata.json", "w") as f:
                json.dump(metadata_data, f, indent=2)
            print("Successfully wrote pr_metadata.json", file=sys.stderr)
        except Exception as e:
            print(f"Failed to parse metadata JSON: {e}\nRaw response: {metadata_response}", file=sys.stderr)
            sys.exit(1)
    else:
        sys.exit(1)

if __name__ == "__main__":
    main()
