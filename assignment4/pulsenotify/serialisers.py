import logging
from django.db import transaction
from rest_framework import serializers
from django.contrib.auth.models import User

logger = logging.getLogger(__name__)

class UserSerialiser(serializers.ModelSerializer):
    password = serializers.CharField(write_only=True)

    class Meta:
        model = User
        fields = ["id", "username", "password", "email"]
        read_only_fields = ["id"]

    @transaction.atomic
    def create(self, validated_data):
        password = validated_data.pop("password")
        username = validated_data.get("username")
        logger.info("op=create username=%s", username)
        user = User.objects.create_user(password=password, **validated_data)
        logger.info("op=create status=success user_id=%s", user.id)
        return user

class SignUpSerialiser(serializers.ModelSerializer):
    password = serializers.CharField(write_only=True)
    
    class Meta:
        model = User
        fields = ["id", "username", "password", "email"]
        read_only_fields = ["id"]

class LoginSerialiser(serializers.ModelSerializer):
    password = serializers.CharField(write_only=True)

    class Meta:
        model = User
        fields = ["id", "username", "password", "email"]
        read_only_fields = ["id"]