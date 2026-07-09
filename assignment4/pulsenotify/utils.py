import jwt
from datetime import datetime, timezone, timedelta
from rest_framework.authentication import BaseAuthentication
from rest_framework.permissions import BasePermission
from rest_framework.exceptions import AuthenticationFailed
from django.contrib.auth.models import User
from .models import UserProfile

class JWTUtil:

    SECRET_KEY = "airtribe-jwt-api-token-for-backend"
    ALGORITHM = "HS256"
    EXPIRY_MINUTES = 30

    @staticmethod
    def create_token(user_id):
        payload = {
            "user_id": user_id,
            "exp": datetime.now(timezone.utc)
            + timedelta(minutes=JWTUtil.EXPIRY_MINUTES),
            "iat": datetime.now(timezone.utc),
        }
        return jwt.encode(payload, JWTUtil.SECRET_KEY, algorithm=JWTUtil.ALGORITHM)

    @staticmethod
    def verify_token(token):
        try:
            payload = jwt.decode(
                token, JWTUtil.SECRET_KEY, algorithms=[JWTUtil.ALGORITHM]
            )
            username = payload["user_id"]
            user = User.objects.get(username=username)
            return {"valid": True, "user_id": payload["user_id"]}
        except jwt.ExpiredSignatureError:
            return {"valid": False, "error": "Token has expired."}
        except jwt.InvalidTokenError:
            return {"valid": False, "error": "Invalid token."}
        
class JWTAuthentication(BaseAuthentication):

    def authenticate(self, request):
        auth_header = request.headers.get("Authorization")
        if not auth_header:
            return None  # No credentials provided; skip to next authenticator

        if not auth_header.startswith("Bearer "):
            raise AuthenticationFailed("Authorization header must use Bearer scheme.")

        token = auth_header[7:]
        result = JWTUtil.verify_token(token)

        if not result["valid"]:
            raise AuthenticationFailed(result["error"])

        try:
            user = User.objects.get(username=result["user_id"])
        except User.DoesNotExist:
            raise AuthenticationFailed("User not found.")

        return (user, token)  # Returning a tuple with a user and the token

    def authenticate_header(self, request):
        return "Bearer"
class IsAdminUser(BasePermission):
    message = "You must be an admin."
    def has_permission(self, request, view):
        auth_header = request.headers.get("Authorization")
        token = auth_header[7:]
        result = JWTUtil.verify_token(token)
        user = User.objects.get(username=result["user_id"])
        return user.profile.role == UserProfile.Role.ADMIN