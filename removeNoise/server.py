# server.py : 연결해 1 보낼 수 있음. 
from http import client
import socket 
import highpassfilter as hpf
import os

host = "192.168.137.27"  # 호스트 ip를 적어주세요 
port = 9999            # 포트번호를 임의로 설정해주세요 

print("ip :" + host)
server_sock = socket.socket(socket.AF_INET) 
server_sock.bind((host, port)) 
server_sock.listen(10) 

print("기다리는 중") 
client_sock, addr = server_sock.accept() 
print('Connected by', addr) 
length = client_sock.recv(1024)
data_transferred = 0
print(type(length)) # length = bytes
nowdir = os.getcwd()

with open(nowdir+"\\"+"beforeHpf.wav", "wb") as f:
    print("File Opened")
    while True:
        # data_transferred+=len(data)
        data = client_sock.recv(4096)
        f.write(data)
        if not data:
            print("no more data")
            break 
        print("FileWriting...")
hpf.highpassfilter()
f.close()

#senddata = hpf.highpassfilter() 
print("파일 받기 완료, ")
#hpf.highpassfilter() 

#hpf.highpassfilter(data)


#client_sock.send(senddata) 

client_sock.close() 
server_sock.close()