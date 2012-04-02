import logging

from pyramid.view import view_config
from pyramid.httpexceptions import HTTPForbidden, HTTPBadRequest

from pings import resources

logger = logging.getLogger(__name__)

@view_config(route_name='get_pings',
             renderer='json', request_method='POST')
def get_pings(request):
    """Called by the client to get a list of addresses to ping."""
    logger.debug('get_pings request: %s', request.POST)
    logger.debug('get_pings request client address: %s', request.client_addr)

    ip_addresses = resources.get_pings()
    return {'token': resources.get_token(),
            'pings': ip_addresses,
            'geoip': resources.get_geoip_data(ip_addresses)}

@view_config(route_name='submit_ping_results',
             renderer='json', request_method='POST')
def submit_ping_results(request):
    """Called by the client to submit the results of the addresses pinged."""
    logger.debug('submit_ping_results request: %s', request.POST)
    
    token = request.POST.get('token')
    if not resources.check_token(token):
        raise HTTPForbidden('Invalid or absent token value.')

    results = request.POST.get('results')
    if results is None:
        raise HTTPBadRequest('No "results" field.')
    resources.store_results(results)
        
    return {'success': True}