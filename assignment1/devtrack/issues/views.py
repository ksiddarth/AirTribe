import json
from rest_framework.decorators import api_view
from rest_framework.views import Request, Response
from pathlib import Path

from devtrack.issues.models import Reporter, Issue, CriticalIssue, LowPriorityIssue

BASE_DIR = Path(__file__).resolve().parent.parent
REPORTERS_FILE = BASE_DIR / "data" / "reporters.json"
ISSUES_FILE = BASE_DIR / "data" / "issues.json"


def intiate_reporter_ids():
    reporters = get_reporters()
    for reporter in reporters:
        Reporter.VALID_REPORTER_ID[reporter.get("id")] = reporter.get("name")

@api_view(["GET", "POST"])
def get_create_reporter(request: Request):
    if request.method == "GET":
        reporters = get_reporters()
        id = request.GET.get("id")
        if id:
            for reporter in reporters:
                if reporter.get("id") == int(id):
                    return Response(data= {"message" : "Reporter found", "reporter" : reporter}, status = 200)  
            return Response(data= {"error" : f"Reporter not found with id : {id}"}, status = 404)
        else:
            return Response(data= {"message" : "Reporters found", "reporters" : reporters}, status = 200)

    elif request.method == "POST":
        data = request.data
        if not Reporter.VALID_REPORTER_ID:
            intiate_reporter_ids()
        reporter = create_reporter(data)
        return Response(data= {"message" : "Reporter has been created", "reporter" : reporter.to_dict()}, status = 201)

@api_view(["GET", "POST"])
def get_create_issue(request: Request):
    if request.method == "GET":
        issues = get_issues()
        id = request.GET.get("id")
        status = request.GET.get("status")
        if id:
            for issue in issues:
                if issue.get("id") == int(id):
                    return Response(data= {"message" : "Issue found", "issue" : issue}, status = 200)
            return Response(data= {"error" : f"Issue not found with id : {id}"}, status = 404)
        elif status:
            filter_issues = []
            for issue in issues:
                if issue.get("status") == status:
                    filter_issues.append(issue)
            if filter_issues:
                return Response({"message": "The issues are", "issues": filter_issues}, status = 200)
            else:
                return Response(data= {"error" : f"Issue not found with status : {status}"}, status = 404)
        else:
            return Response({"message": "The issues are", "issues": issues}, status = 200)

    elif request.method == "POST":
        data = request.data
        if not Reporter.VALID_REPORTER_ID:
            intiate_reporter_ids()
        issue = create_issue(data)
        return Response(data= {"message" : issue.describe(), "issue" : issue.to_dict()}, status = 201)


def get_reporters():
    with open(REPORTERS_FILE, "r") as file:
        content = file.read()
        return json.loads(content) if content.strip() else []

def create_reporter(data: dict):
    reporter = Reporter(
        id = int(data["id"]),
        name = data["name"],
        email = data["email"],
        team = data["team"]
    )
    Reporter.VALID_REPORTER_ID[reporter.id] = reporter.name
    reporters = get_reporters()
    reporters.append(reporter.to_dict())
    with open(REPORTERS_FILE, "w") as file:
        json.dump(reporters, file, indent = 4)
    return reporter

def get_issues():
    with open(ISSUES_FILE, "r") as file:
        content = file.read()
        return json.loads(content) if content.strip() else []

def create_issue(data: dict):
    priority = data["priority"]
    issue = None
    if priority == "CRITICAL":
        issue = CriticalIssue(
            id = int(data["id"]),
            title = data["title"],
            description = data["description"],
            status = data["status"],
            priority = priority,
            reporter_id = int(data["reporter_id"]),
        )
    elif priority == "LOW":
        issue = LowPriorityIssue(
            id = int(data["id"]),
            title = data["title"],
            description = data["description"],
            status = data["status"],
            priority = priority,
            reporter_id = int(data["reporter_id"]),
        )
    else:
        issue = Issue(
            id = int(data["id"]),
            title = data["title"],
            description = data["description"],
            status = data["status"],
            priority = priority,
            reporter_id = int(data["reporter_id"]),
        )
    issues = get_issues()
    issues.append(issue.to_dict())
    with open(ISSUES_FILE, "w") as file:
        json.dump(issues, file, indent = 4)
    return issue
