import secrets
from django.db.models.signals import post_save
from django.dispatch import receiver
from django.contrib.auth.models import User
from .models import Company


 
@receiver(post_save, sender=User)
def create_company_profile(sender, instance, created, **kwargs):
    company_name = getattr(instance, '_temporary_company_name', instance.email)
    if created:
        Company.objects.create(
            user=instance,
            company_name=company_name,
            api_key=secrets.token_urlsafe(32)
        )