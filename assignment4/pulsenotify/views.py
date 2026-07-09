from rest_framework.views import APIView
from rest_framework import permissions, status
from rest_framework.response import Response
from .serialisers import SignUpSerialiser
from .utils import JWTUtil
from django.contrib.auth.models import User


class SignUpView(APIView):
    permission_classes = []

    def post(self, request, *args, **kwargs):
        username = request.data.get("username")
        serialiser = SignUpSerialiser(data=request.data)
        if not serialiser.is_valid():
            return Response(data=serialiser.errors, status=401)
        user = serialiser.save()
        jw = JWTUtil.create_token(user.username)
        return Response(
            data={"username": user.username, "access": jw, "role": user.profile.role}, status=201)


class LoginView(APIView):
    permission_classes = []

    def post(self, request, *args, **kwargs):
        username = request.data.get("username")
        password = request.data.get("password")
        user = User.objects.get(username=username)  
        if not user:
            return Response(data={"error": "User not found"}, status=401)
        if user.password != password:
            return Response(data={"error": "Invalid credentials"}, status=401)
        jw = JWTUtil.create_token(user.id)
        return Response(
            data={"username": user.username, "access": jw, "role": user.profile.role}, status=200)