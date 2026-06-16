import logging
from rest_framework import serializers
from django.contrib.auth.models import User
from teamboard.models import KBEntry, QueryLog

logger = logging.getLogger(__name__)

class UserSerialiser(serializers.ModelSerializer):
    password = serializers.CharField(write_only=True)

    class Meta:
        model = User
        fields = ["id", "username", "password", "email"]
        read_only_fields = ["id"]

class SignUpSerialiser(serializers.ModelSerializer):
    password = serializers.CharField(write_only=True)
    
    class Meta:
        model = User
        fields = ["id", "username", "password", "email"]
        read_only_fields = ["id"]
        
    def create(self, validated_data):
        company_name = self.initial_data["company_name"]
        user = User(**validated_data)
        user._temporary_company_name = company_name 
        user.save()
        logger.info("op=create status=success user_id=%s", user.id)
        return user
    
class KBSerialiser(serializers.ModelSerializer):

    class Meta:
        model = KBEntry
        fields = ["id", "question", "answer", "category"]
        read_only_fields = ["id"]

class QuerSeraliser(serializers.ModelSerializer):
    
    class Meta:
        model = QueryLog
        fields = ["id", "company", "search_term", "results_count", "queried_at"]
        read_only_fields = ["id"]