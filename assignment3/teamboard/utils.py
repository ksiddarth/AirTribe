import logging
import jwt
from datetime import datetime, timezone, timedelta
from rest_framework.authentication import BaseAuthentication
from rest_framework.permissions import BasePermission
from rest_framework.exceptions import AuthenticationFailed
from teamboard.models import User, Company

logger = logging.getLogger(__name__)

class JWTUtil:

    SECRET_KEY = "airtribe-jwt-api-token-for-backend"
    ALGORITHM = "HS256"
    EXPIRY_MINUTES = 30

    @staticmethod
    def create_token(user_id, api_key):
        logger.info("op=create_token user_id=%s", user_id)
        payload = {
            "user_id": user_id,
            "api_key": api_key,
            "exp": datetime.now(timezone.utc)
            + timedelta(minutes=JWTUtil.EXPIRY_MINUTES),
            "iat": datetime.now(timezone.utc),
        }
        return jwt.encode(payload, JWTUtil.SECRET_KEY, algorithm=JWTUtil.ALGORITHM)

    @staticmethod
    def verify_token(token):
        logger.info("op=verify_token")
        try:
            payload = jwt.decode(
                token, JWTUtil.SECRET_KEY, algorithms=[JWTUtil.ALGORITHM]
            )
            username = payload["user_id"]
            api_key = payload["api_key"]
            user = User.objects.get(username=username)
            if user.company.api_key == api_key:
                logger.info("op=verify_token status=success user_id=%s", )
                return {"valid": True, "user_id": payload["user_id"]}
            else:
                logger.warning("op=verify_token status=failed reason=wrong api_key")
            return {"valid": False, "error": "JWT Token is invalid for the given user."}
        except jwt.ExpiredSignatureError:
            logger.warning("op=verify_token status=failed reason=token_expired")
            return {"valid": False, "error": "Token has expired."}
        except jwt.InvalidTokenError:
            logger.warning("op=verify_token status=failed reason=invalid_token")
            return {"valid": False, "error": "Invalid token."}
        
class JWTAuthentication(BaseAuthentication):

    def authenticate(self, request):
        auth_header = request.headers.get("Authorization")
        logger.info("op=authenticate path=%s", request.path)
        if not auth_header:
            logger.info("op=authenticate status=skipped reason=no_credentials path=%s", request.path)
            return None  # No credentials provided; skip to next authenticator

        if not auth_header.startswith("Bearer "):
            logger.warning("op=authenticate status=failed reason=invalid_scheme path=%s", request.path)
            raise AuthenticationFailed("Authorization header must use Bearer scheme.")

        token = auth_header[7:]
        result = JWTUtil.verify_token(token)

        if not result["valid"]:
            logger.warning("op=authenticate status=failed reason=%s path=%s", result["error"], request.path)
            raise AuthenticationFailed(result["error"])

        try:
            user = User.objects.get(username=result["user_id"])
        except User.DoesNotExist:
            logger.warning("op=authenticate status=failed reason=user_not_found user_id=%s", result["user_id"])
            raise AuthenticationFailed("User not found.")

        logger.info("op=authenticate status=success user_id=%s path=%s", user.id, request.path)
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
        return user.company.role == Company.Role.ADMIN