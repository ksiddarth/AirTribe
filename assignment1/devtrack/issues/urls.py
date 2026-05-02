from django.urls import path

from devtrack.issues.views import get_create_reporter, get_create_issue

urlpatterns = [
    path("reporters/", get_create_reporter),
    path("issues/", get_create_issue),
]