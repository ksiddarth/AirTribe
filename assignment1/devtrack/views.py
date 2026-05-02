from rest_framework.decorators import api_view
from rest_framework.views import Request, Response


@api_view(["GET"])
def add_two_numbers(request: Request):
    a = int(request.GET.get("a"))
    b = int(request.GET.get("b"))
    return Response(data={"sum": a + b}, status=200)