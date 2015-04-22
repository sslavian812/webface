# -*- coding: utf-8 -*-
import json
import os
import urllib2
import sys

index_url = "http://localhost:9200/vk"

path = ".\\data\\"

if len(sys.argv) != 0:
    path = sys.argv[1]

opener = urllib2.build_opener(urllib2.HTTPHandler)


#creating index
request = urllib2.Request(index_url)
request.get_method = lambda: 'PUT'
opener.open(request)
print "creating index - DONE"

#adding type user and mapping
user_mapping = json.loads(open('_mapping_user.json').read().decode('utf-8'))
request = urllib2.Request(index_url + '/_mapping/user',
                          data=unicode(json.dumps(user_mapping, ensure_ascii=False)))
request.get_method = lambda: 'PUT'
opener.open(request)


#adding type post and mapping
post_mapping = json.loads(open('_mapping_post.json').read().decode('utf-8'))
request = urllib2.Request(index_url + '/_mapping/post',
                          data=unicode(json.dumps(post_mapping, ensure_ascii=False)))
request.get_method = lambda: 'PUT'
opener.open(request)


#adding type group and mapping
group_mapping = json.loads(open('_mapping_group.json').read().decode('utf-8'))
request = urllib2.Request(index_url+'/_mapping/group',
                          data=unicode(json.dumps(group_mapping, ensure_ascii=False)))
request.get_method = lambda: 'PUT'
opener.open(request)

print "adding mappings - DONE"