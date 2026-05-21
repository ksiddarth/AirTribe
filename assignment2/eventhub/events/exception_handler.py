from rest_framework.views import exception_handler
from rest_framework.response import Response
import traceback

def custom_exception_handler(exc, context):
    response = exception_handler(exc, context)

    if response is None:
        traceback.print_exc()
        return Response({"error": str(exc)}, status=400)

    return response