# Description
Java Spring Boot back-end application for querying the USD to BRL exchange rate via an API.
## Endpoints
- GET `/moeda/{startDate}&{endDate}` - Exchange rate history for a given period
- GET `/moeda/atual` - Current exchange rate
- GET `/moeda/menor-atual/{startDate}&{endDate}` - Exchange rates in a given period which are lower than the current one

** All dates are in "MM-dd-yyyy" format.
