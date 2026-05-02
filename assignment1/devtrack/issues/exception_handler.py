from rest_framework.views import exception_handler
from rest_framework.response import Response
import traceback

'''class ExceptionHandler:
    def __init__(self, get_response):
        self.get_response = get_response

    def __call__(self, request):
        try:
            response = self.get_response(request)
            return response
        except Exception as e:
            return JsonResponse({"error": str(e)}, status=400)
'''

def custom_exception_handler(exc, context):
    response = exception_handler(exc, context)

    if response is None:
        traceback.print_exc()
        return Response({"error": str(exc)}, status=400)

    return response