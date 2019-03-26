# michael-config-server
---
Config server is used to add config files on cloud. In this case, the process can read config files through this.

## config server rule
Actually, there are many ways to reach this.
* local file repository
* github repository
* database repository
Here we use github as cloud repository.

```
server:
   port: 7700

spring:
   cloud:
      config:
         server:
            git:
               uri: https://github.com/hexin1250/application-config
               skipSslValidation: true
               timeout: 4
               force-pull: true
   application:
      name: config
```
