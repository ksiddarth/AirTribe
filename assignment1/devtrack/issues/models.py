from abc import ABC, abstractmethod
from django.utils import timezone
from django.http import Http404


class BaseEntity(ABC):
    @abstractmethod
    def validate(self):
        pass

    def to_dict(self):
        return {
            key: value
            for key, value in self.__dict__.items()
        }

class Reporter(BaseEntity):
    VALID_REPORTER_ID = {}
    def __init__(self, id: int, name: str, email: str, team: str):
        self.id = id
        self.name = name
        self.email = email
        self.team = team
        self.validate()

    def validate(self):
        if not self.id:
            raise ValueError('Reporter Id must be passed')
        if not self.name:
            raise ValueError('Name cannot be empty')
        if '@' not in self.email:
            raise ValueError('Invalid email')
        if Reporter.VALID_REPORTER_ID and (self.id in Reporter.VALID_REPORTER_ID.keys()):
            raise ValueError(f"Reporter Id already exists and is used by {Reporter.VALID_REPORTER_ID.get(self.id)}")


class Issue(BaseEntity):
    VALID_PRIORITY = ["LOW", "MEDIUM", "HIGH", "CRITICAL"]
    VALID_STATUS = ["OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED"]
    def __init__(self, id: int, title: str, description: str, status: str, priority: str, reporter_id: int):
        self.id = id
        self.title = title
        self.description = description
        self.status = status
        self.priority = priority
        self.reporter_id = reporter_id
        self.created_at = str(timezone.now())
        self.validate()

    def validate(self):
        if not self.id:
            raise ValueError('Reporter Id must be passed')
        if not self.title:
            raise ValueError('Name cannot be empty')
        if self.priority not in Issue.VALID_PRIORITY:
            raise ValueError('Priority should be valid')
        if self.status not in Issue.VALID_STATUS:
            raise ValueError('Status should be valid')  
        if self.reporter_id not in  Reporter.VALID_REPORTER_ID:
            raise Http404('Repoprter id should be valid and existing')
    
    def describe(self):
        return f"{self.title} [{self.priority}]"


class CriticalIssue(Issue):

    def describe(self):
        return f"[URGENT] {self.title} — needs immediate attention"

class LowPriorityIssue(Issue):

    def describe(self):
        return f"{self.title} — low priority, handle when free"