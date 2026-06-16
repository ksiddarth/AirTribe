import logging
from django.db import transaction
from django.db.models import Q, Count
from rest_framework.views import APIView, Request, Response
from teamboard.serialisers import SignUpSerialiser, KBSerialiser
from teamboard.models import User, KBEntry, QueryLog, Company
from teamboard.utils import JWTUtil, JWTAuthentication, IsAdminUser

logger = logging.getLogger(__name__)

class SignUpView(APIView):

    authentication_classes = []
    permission_classes = []

    def post(self, request):
        username = request.data.get("username")
        logger.info("op=post view=SignUpView username=%s", username)
        serialiser = SignUpSerialiser(data=request.data)
        if not serialiser.is_valid():
            logger.warning("op=post view=SignUpView status=failed reason=validation_error username=%s", username)
            return Response(data=serialiser.errors, status=401)
        user = serialiser.save()
        logger.info("op=post view=SignUpView status=success user_id=%s", user.id)
        jw = JWTUtil.create_token(user.username, user.company.api_key)
        return Response(
            data={"username": user.username, "company_name": user.company.company_name, 
                  "api_key": user.company.api_key, "access": jw}, status=201)
    
class LoginView(APIView):

    authentication_classes = []
    permission_classes = []

    def post(self, request):
        username = request.data.get("username")
        password = request.data.get("password")
        try:
            user = User.objects.get(username=username, password=password)
            jw = JWTUtil.create_token(user.username, user.company.api_key)
            return Response(data={"access": jw, "company_name": user.company.company_name, 
                  "api_key": user.company.api_key}, status=201)
        except User.DoesNotExist:
            return Response(data={"message": "either username or password is incorrect"}, status=401)
        

class SearchView(APIView):

    authentication_classes = [JWTAuthentication]
    
    @transaction.atomic
    def post(self, req):
        search = req.data.get("search")
        if search:
            company_name = req.user.company.company_name
            comp = Company.objects.get(company_name=company_name)
            results = set()
            for s in search.split():
                if s.upper() not in ["EXPLAIN", "TELL", "HOW", "WHY", "WHAT", "WHO", "TO", "ME", "THIS"]:
                    query = Q(question__icontains=s) | Q(answer__icontains=s)
                    x = KBEntry.objects.filter(query)
                    QueryLog.objects.create(
                        company=comp,
                        search_term=s,
                        results_count=len(x)
                    )
                    for r in x:
                        results.add(r)        
            seralizer = KBSerialiser(results, many=True)
            return Response(data={"search":search, "count": len(results), "results":seralizer.data}, status=200)
        else:
            return Response(data={"message":"Serach query given is blank"}, status=400)
        

class StaticsView(APIView):

    authentication_classes = [JWTAuthentication]
    permission_classes = [IsAdminUser]

    def get(self, req):
        total_queries = QueryLog.objects.aggregate(total=Count('id'))['total']
        active_companies = QueryLog.objects.values('company').distinct().count()
        top_search_terms = list(
            QueryLog.objects.values('search_term')
            .annotate(count=Count('id'))
            .order_by('-count')[:5]
        )
        return Response(
            data={
                "total_queries": total_queries,
                "active_companies": active_companies,
                "top_search_terms": top_search_terms,
            },
            status=200,
        )

