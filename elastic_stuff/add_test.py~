# -*- coding: utf-8 -*-
import json
import os
import urllib2
import sys

# def encoded_dict(in_dict):
#     out_dict = {}
#     for k, v in in_dict.iteritems():
#         if isinstance(v, unicode):
#             v = v.encode('utf-8')
#         elif isinstance(v, str):
#             # Must be encoded in UTF-8
#             v.decode('utf-8')
#         out_dict[k] = v
#     return out_dict

def convert_obj(in_dict):
    str_post = {}
    for k, v in in_dict.iteritems():
        str_post[k] = unicode(v).encode('utf-8')
    return str_post


index_url = "http://localhost:9200/vk"

path = ".\\data\\"

if len(sys.argv) != 0:
    path = sys.argv[1]

opener = urllib2.build_opener(urllib2.HTTPHandler)

#deleting index
request = urllib2.Request(index_url)
request.get_method = lambda: 'DELETE'
opener.open(request)
print "deleting index - DONE"


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

files = os.listdir(path)


for file in files:
    s = json.loads(open(path + file).read().decode('utf-8'))

    user_id = s['id']

    user = {}
    user['id'] = s['id']

    user.update(s['generalInformation'])
    user['sub_groups_count'] = s['groupInformation']['sub_groups_count']
    user['groups'] = s['groupInformation']['groups']


    user['sub_user_count'] = s['sub_user_count']
    user['followers_count'] = s['followers_count']
    user['wall_count'] = s['wall_count']
    user['albums_count'] = s['albums_count']

    user['friends'] = s['friends']
    user['subscriptions'] = s['subscriptions']
    user['university_name'] = s['university_name']
    user['first_name'] = s['first_name']
    user['last_name'] = s['last_name']



    # add user to index
    request = urllib2.Request(url=(index_url + "/user/"),
                    data=unicode(json.dumps(user, ensure_ascii=False, encoding="utf-8")).encode("utf-8"))
    request.get_method = lambda: 'POST'
    opener.open(request)


    # add each post to index with specified parent id
    posts = s['wall_content']
    for post in posts:
        request = urllib2.Request(url=(index_url + "/post/"+str(post['id'])+"?parent=" + user_id),
                    data=json.dumps(convert_obj(post)), headers={"Content-Type": "application/json"})
        request.get_method = lambda: 'PUT'
        opener.open(request)
        pass


    # add each group information to index
    groups = s['groupInformation']['groups_detail']
    for group in groups:
        request = urllib2.Request(url=(index_url + "/group/"+str(group['id'])+"?parent=" + user_id),
                    data=json.dumps(convert_obj(group)), headers={"Content-Type": "application/json"})
        request.get_method = lambda: 'PUT'
        opener.open(request)
        pass

print "adding all documents - DONE"
# writing this script brought a multitude of pain and anger. be careful

