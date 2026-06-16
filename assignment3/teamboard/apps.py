from django.apps import AppConfig

class TeamboardAppConfig(AppConfig):
    name = 'teamboard'

    def ready(self):
        import teamboard.signals