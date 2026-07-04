from django.apps import AppConfig


class PulsenotifyConfig(AppConfig):
    default_auto_field = "django.db.models.BigAutoField"
    name = "pulsenotify"

    def ready(self):
        import pulsenotify.signals
