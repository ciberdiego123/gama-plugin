FROM ubuntu 

RUN apt update && apt install -y apache2 openssh-server 
RUN echo "www-data:www-data" | chpasswd
RUN sed -i 's;www-data:x:33:33:www-data:/var/www:/usr/sbin/nologin;www-data:x:33:33:www-data:/var/www:/bin/bash;' /etc/passwd
RUN mkdir -p /var/run/sshd

CMD    ["/usr/sbin/sshd", "-D"]
