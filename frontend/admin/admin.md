---
title: admin
language_tabs:
  - shell: Shell
  - http: HTTP
  - javascript: JavaScript
  - ruby: Ruby
  - python: Python
  - php: PHP
  - java: Java
  - go: Go
toc_footers: []
includes: []
search: true
code_clipboard: true
highlight_theme: darkula
headingLevel: 2
generator: "@tarslib/widdershins v4.0.30"

---

# admin

Base URLs:

# Authentication

- HTTP Authentication, scheme: bearer

# 登录/登出

## POST 后台登出

POST /admin/log/logout

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||none|
|» msg|string|true|none||none|
|» data|object¦null|false|none||none|

## POST 后台登录

POST /admin/log/login

> Body 请求参数

```json
{
    "email": "i1cl5v22@yahoo.com.cn",
    "password": "NVkhh0wU7YPiVr4"
}
```

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|body|body|object| 是 ||none|
|» email|body|string| 是 | 邮箱|none|
|» password|body|string| 是 | 密码|none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": {
    "id": 0,
    "username": "string",
    "email": "string",
    "token": "string"
  }
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||none|
|» msg|string|true|none||none|
|» data|object¦null|true|none||none|
|»» id|integer|true|none||none|
|»» username|string|true|none||none|
|»» email|string|true|none||none|
|»» token|string|true|none||none|

# 数据接口

## GET 获取数据概览

GET /admin/data/overview

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|startDate|query|string| 是 ||起始日期|
|endDate|query|string| 是 ||终止日期|
|granularity|query|string| 是 ||粒度（天/周/月）day/week/month|
|areaId|query|integer| 否 ||区域id|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": {
    "startDate": "string",
    "endDate": "string",
    "granularity": "string",
    "areaId": "string",
    "series": [
      {
        "time": "string",
        "orderCount": 0,
        "revenue": 0
      }
    ]
  }
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||none|
|» msg|string|true|none||none|
|» data|object¦null|false|none||none|
|»» startDate|string|true|none|起始日期|none|
|»» endDate|string|true|none|终止日期|none|
|»» granularity|string|true|none|粒度（天/周/月）|day/week/month|
|»» areaId|string¦null|false|none|区域id|none|
|»» series|[object]¦null|false|none||none|
|»»» time|string¦null|true|none|时间|none|
|»»» orderCount|integer¦null|true|none|订单量|none|
|»»» revenue|number¦null|true|none|收入|none|

## GET 获取实时数据

GET /admin/data/liveData

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|date|query|string| 否 ||日期YYYY-MM-DD|
|areaId|query|string| 否 ||区域id|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": {
    "updatedAt": "2019-08-24T14:15:22Z",
    "todayOrders": 0,
    "todayRevenue": 0,
    "onlineScooters": 0,
    "faultScooters": 0,
    "areaId": 0
  }
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||none|
|» msg|string|true|none||none|
|» data|object¦null|true|none||none|
|»» updatedAt|string(date-time)|true|none|更新时间|none|
|»» todayOrders|integer|true|none|总订单数|none|
|»» todayRevenue|number|true|none|总收入|none|
|»» onlineScooters|integer|true|none|在线车辆数量|none|
|»» faultScooters|integer¦null|true|none|故障车辆数量|none|
|»» areaId|integer¦null|false|none|区域ID|none|

# 定价接口

## GET 获取定价策略

GET /admin/pricingRule/getRules

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": {
    "id": 1,
    "pricePerMin": "0.00",
    "basePrice": "0.00",
    "billingInterval": "30"
  }
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||none|
|» msg|string|true|none||none|
|» data|object¦null|true|none||none|
|»» id|integer|true|none||主键ID|
|»» pricePerMin|number¦null|false|none||每分钟单价|
|»» basePrice|number¦null|false|none||起步价|
|»» billingInterval|integer¦null|false|none||计费间隔（分钟）|

## POST 修改定价策略

POST /admin/pricingRule/editRules

> Body 请求参数

```json
{
  "pricePerMin": "0.00",
  "basePrice": "0.00",
  "billingInterval": "30"
}
```

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|body|body|object| 是 ||none|
|» pricePerMin|body|number¦null| 否 ||每分钟单价|
|» basePrice|body|number¦null| 否 ||起步价|
|» billingInterval|body|integer¦null| 否 ||计费间隔（分钟）|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||none|
|» msg|string|true|none||none|
|» data|object¦null|false|none||none|

# 套餐接口

## GET 获取套餐列表

GET /admin/packages/getPackageList

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|page|query|integer| 否 ||第几页|
|pageSize|query|integer| 否 ||每页容量|
|status|query|string| 否 ||状态|
|keyword|query|string| 否 ||关键词 用于搜索|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": {
    "list": [
      {
        "id": 1,
        "title": "string",
        "description": "string",
        "price": 0,
        "type": "1",
        "createTime": "CURRENT_TIMESTAMP"
      }
    ]
  }
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||none|
|» msg|string|true|none||none|
|» data|object|true|none||none|
|»» list|[object]|true|none||none|
|»»» id|integer|true|none||主键ID|
|»»» title|string|true|none||套餐标题|
|»»» description|string¦null|false|none||套餐描述|
|»»» price|number|true|none||价格|
|»»» type|integer¦null|false|none||类型：1-月卡，2-季卡，3-年卡|
|»»» createTime|string(date-time)¦null|false|none||创建时间|

## POST 新增套餐

POST /admin/packages/addPackage

> Body 请求参数

```json
{
  "title": "string",
  "description": "string",
  "price": 0,
  "type": "1",
  "createTime": "CURRENT_TIMESTAMP"
}
```

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|body|body|object| 是 ||none|
|» title|body|string| 是 ||套餐标题|
|» description|body|string¦null| 否 ||套餐描述|
|» price|body|number| 是 ||价格|
|» type|body|integer¦null| 否 ||类型：1-月卡，2-季卡，3-年卡|
|» createTime|body|string(date-time)¦null| 否 ||创建时间|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||none|
|» msg|string|true|none||none|
|» data|object¦null|true|none||none|

## POST 编辑套餐

POST /admin/packages/editPackage

> Body 请求参数

```json
{
  "id": 1,
  "title": "string",
  "description": "string",
  "price": 0,
  "type": "1",
  "editTime": "2019-08-24T14:15:22Z"
}
```

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|body|body|object| 是 ||none|
|» id|body|integer| 是 ||主键ID|
|» title|body|string¦null| 是 ||套餐标题|
|» description|body|string¦null| 否 ||套餐描述|
|» price|body|number| 是 ||价格|
|» type|body|integer¦null| 否 ||类型：1-月卡，2-季卡，3-年卡|
|» editTime|body|string(date-time)| 是 ||编辑时间|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||none|
|» msg|string|true|none||none|
|» data|object¦null|true|none||none|

## DELETE 删除套餐

DELETE /admin/packages/deletePackage

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|packageId|query|integer| 是 ||套餐主键ID|
|title|query|string| 否 ||套餐标题|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||none|
|» msg|string|true|none||none|
|» data|object¦null|false|none||none|

# 片区接口

## GET 获取片区列表

GET /admin/zones/getZoneList

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|page|query|integer| 否 ||none|
|pagesize|query|integer| 否 ||none|
|keyword|query|string| 否 ||none|
|dispatcherId|query|integer| 否 ||none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": {
    "areaList": [
      {
        "id": 1,
        "name": "string",
        "polygon": "string",
        "createTime": "CURRENT_TIMESTAMP",
        "dispatchers": [
          {
            "id": null,
            "name": null
          }
        ]
      }
    ],
    "page": 0,
    "pageSize": 0,
    "total": 0
  }
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||none|
|» msg|string|true|none||none|
|» data|object|true|none||none|
|»» areaList|[object]|true|none||none|
|»»» id|integer|true|none||主键ID|
|»»» name|string|true|none||区域名称|
|»»» polygon|string¦null|true|none||多边形范围坐标|
|»»» createTime|string(date-time)¦null|false|none||创建时间|
|»»» dispatchers|[object]|true|none||none|
|»»»» id|integer|true|none||主键ID|
|»»»» name|string|true|none||姓名|
|»» page|integer|true|none||none|
|»» pageSize|integer|true|none||none|
|»» total|integer|true|none||none|

## POST 新增片区（绘制）

POST /admin/zones/addZone

> Body 请求参数

```json
{
  "dispatchers": [
    {
      "id": 1,
      "name": "string"
    }
  ],
  "id": 1,
  "name": "string",
  "polygon": "string"
}
```

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|body|body|object| 是 ||none|
|» dispatchers|body|[object]| 是 ||none|
|»» id|body|integer| 是 ||主键ID|
|»» name|body|string| 是 ||姓名|
|» id|body|integer| 是 ||主键ID|
|» name|body|string| 是 ||区域名称|
|» polygon|body|string¦null| 否 ||多边形范围坐标|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": {
    "id": 1,
    "name": "string",
    "polygon": "string",
    "dispatchers": [
      {
        "id": 1,
        "name": "string"
      }
    ],
    "polygonPointCount": 0,
    "createdBy": "string"
  }
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||none|
|» msg|string|true|none||none|
|» data|object|true|none||none|
|»» id|integer|true|none||主键ID|
|»» name|string|true|none||区域名称|
|»» polygon|string¦null|false|none||多边形范围坐标|
|»» dispatchers|[object]|true|none||none|
|»»» id|integer|true|none||主键ID|
|»»» name|string|true|none||姓名|
|»» polygonPointCount|integer|false|none||none|
|»» createdBy|string|false|none||none|

## GET 获取片区详情

GET /admin/zones/getZoneDetail

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|areaId|query|integer| 是 ||none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": {
    "id": 1,
    "name": "string",
    "polygon": "string",
    "dispatchers": [
      {
        "id": 1,
        "name": "string",
        "email": "string",
        "areaId": -2147483648
      }
    ],
    "vehicleCount": 0
  }
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||none|
|» msg|string|true|none||none|
|» data|object¦null|true|none||none|
|»» id|integer|true|none||主键ID|
|»» name|string|true|none||区域名称|
|»» polygon|string¦null|false|none||多边形范围坐标|
|»» dispatchers|[object]|true|none||none|
|»»» id|integer|true|none||主键ID|
|»»» name|string|true|none||姓名|
|»»» email|string¦null|false|none||邮箱|
|»»» areaId|integer¦null|false|none||所属区域ID|
|»» vehicleCount|integer|false|none||none|

## POST 编辑片区

POST /admin/zones/editZone

> Body 请求参数

```json
{
  "id": 1,
  "name": "string",
  "polygon": "string",
  "createTime": "CURRENT_TIMESTAMP"
}
```

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|body|body|object| 是 ||none|
|» id|body|integer| 是 ||主键ID|
|» name|body|string| 否 ||区域名称|
|» polygon|body|string¦null| 否 ||多边形范围坐标|
|» createTime|body|string(date-time)¦null| 否 ||创建时间|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": {
    "zone": {
      "id": 1,
      "name": "string",
      "polygon": "string",
      "createTime": "CURRENT_TIMESTAMP"
    },
    "dispatchers": [
      {
        "id": 1,
        "name": "string"
      }
    ]
  }
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||code|
|» msg|string|true|none||none|
|» data|object¦null|true|none||none|
|»» zone|object¦null|true|none||none|
|»»» id|integer|true|none||主键ID|
|»»» name|string|true|none||区域名称|
|»»» polygon|string¦null|false|none||多边形范围坐标|
|»»» createTime|string(date-time)¦null|false|none||创建时间|
|»» dispatchers|[object]¦null|true|none||none|
|»»» id|integer|true|none||主键ID|
|»»» name|string|true|none||姓名|

## DELETE 删除片区

DELETE /admin/zones/deleteZone

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|areaId|query|integer| 是 ||none|
|name|query|string| 否 ||none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||none|
|» msg|string|true|none||none|
|» data|object¦null|true|none||none|

# 禁停区接口

## GET 获取禁停区列表

GET /admin/noParkingZones/getZoneList

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|page|query|integer| 否 ||none|
|pagesize|query|integer| 否 ||none|
|keyword|query|string| 否 ||none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": {
    "areaList": [
      {
        "id": 1,
        "name": "string",
        "polygon": "string",
        "status": "1"
      }
    ],
    "page": 0,
    "pageSize": 0,
    "total": 0,
    "vehicleCount": 0
  }
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||none|
|» msg|string|true|none||none|
|» data|object|true|none||none|
|»» areaList|[object]|true|none||none|
|»»» id|integer|true|none||主键ID|
|»»» name|string¦null|false|none||禁停区名称|
|»»» polygon|string¦null|false|none||禁停多边形范围坐标|
|»»» status|integer¦null|false|none||状态：1-启用，0-禁用|
|»» page|integer|true|none||none|
|»» pageSize|integer|true|none||none|
|»» total|integer|true|none||none|
|»» vehicleCount|integer|true|none||none|

## POST 新增禁停区（绘制）

POST /admin/noParkingZones/addZone

> Body 请求参数

```json
{
  "name": "string",
  "polygon": "string",
  "status": "1"
}
```

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|body|body|object| 是 ||none|
|» name|body|string¦null| 否 ||禁停区名称|
|» polygon|body|string¦null| 否 ||禁停多边形范围坐标|
|» status|body|integer¦null| 否 ||状态：1-启用，0-禁用|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": {
    "polygonPointCount": 0,
    "createdBy": "string",
    "id": 1,
    "name": "string",
    "polygon": "string",
    "status": "1",
    "createTime": "CURRENT_TIMESTAMP"
  }
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||none|
|» msg|string|true|none||none|
|» data|object|true|none||none|
|»» polygonPointCount|integer|false|none||none|
|»» createdBy|string|false|none||none|
|»» id|integer|true|none||主键ID|
|»» name|string¦null|false|none||禁停区名称|
|»» polygon|string¦null|false|none||禁停多边形范围坐标|
|»» status|integer¦null|false|none||状态：1-启用，0-禁用|
|»» createTime|string(date-time)¦null|false|none||创建时间|

## POST 编辑禁停区

POST /admin/noParkingZones/editZone

> Body 请求参数

```json
{
  "id": 1,
  "name": "string",
  "polygon": "string",
  "status": "1",
  "createTime": "CURRENT_TIMESTAMP"
}
```

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|body|body|object| 是 ||none|
|» id|body|integer| 是 ||主键ID|
|» name|body|string¦null| 否 ||禁停区名称|
|» polygon|body|string¦null| 否 ||禁停多边形范围坐标|
|» status|body|integer¦null| 否 ||状态：1-启用，0-禁用|
|» createTime|body|string(date-time)¦null| 否 ||创建时间|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": {
    "id": 1,
    "name": "string",
    "polygon": "string",
    "status": "1",
    "createTime": "CURRENT_TIMESTAMP"
  }
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||code|
|» msg|string|true|none||none|
|» data|object¦null|true|none||none|
|»» id|integer|true|none||主键ID|
|»» name|string¦null|false|none||禁停区名称|
|»» polygon|string¦null|false|none||禁停多边形范围坐标|
|»» status|integer¦null|false|none||状态：1-启用，0-禁用|
|»» createTime|string(date-time)¦null|false|none||创建时间|

## DELETE 删除禁停区

DELETE /admin/noParkingZones/deleteZone

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|NoParkingAreaId|query|integer| 是 ||none|
|name|query|string| 否 ||none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||none|
|» msg|string|true|none||none|
|» data|object¦null|true|none||none|

# 调度员管理接口

## GET 获取调度员列表

GET /admin/dispatchers/getDispatcherList

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|page|query|integer| 否 ||none|
|pagesize|query|integer| 否 ||none|
|keyword|query|string| 否 ||none|
|areaId|query|integer| 否 ||none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": {
    "dispatcherList": [
      {
        "id": 1,
        "name": "string",
        "email": "string",
        "areaId": -2147483648,
        "createTime": "CURRENT_TIMESTAMP"
      }
    ],
    "page": 0,
    "pageSize": 0,
    "total": 0
  }
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||none|
|» msg|string|true|none||none|
|» data|object¦null|true|none||none|
|»» dispatcherList|[object]|true|none||none|
|»»» id|integer|true|none||主键ID|
|»»» name|string|true|none||姓名|
|»»» email|string¦null|false|none||邮箱|
|»»» areaId|integer¦null|false|none||所属区域ID|
|»»» createTime|string(date-time)¦null|false|none||创建时间|
|»» page|integer|true|none||none|
|»» pageSize|integer|true|none||none|
|»» total|integer|true|none||none|

## POST 新增调度员

POST /admin/dispatchers/addDispatcher

> Body 请求参数

```json
{
  "id": 1,
  "name": "string",
  "password": "string",
  "email": "string",
  "areaId": -2147483648
}
```

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|body|body|object| 是 ||none|
|» id|body|integer| 是 ||主键ID|
|» name|body|string| 是 ||姓名|
|» password|body|string| 是 ||密码|
|» email|body|string¦null| 否 ||邮箱|
|» areaId|body|integer¦null| 否 ||所属区域ID|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": {
    "id": 1,
    "name": "string",
    "email": "string",
    "areaId": -2147483648,
    "createTime": "CURRENT_TIMESTAMP"
  }
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||none|
|» msg|string|true|none||none|
|» data|object|true|none||none|
|»» id|integer|true|none||主键ID|
|»» name|string|true|none||姓名|
|»» email|string¦null|false|none||邮箱|
|»» areaId|integer¦null|false|none||所属区域ID|
|»» createTime|string(date-time)¦null|false|none||创建时间|

## POST 编辑调度员

POST /admin/dispatchers/editDispatcher

> Body 请求参数

```json
{
  "id": 1,
  "name": "string",
  "password": "string",
  "email": "string",
  "areaId": -2147483648
}
```

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|body|body|object| 是 ||none|
|» id|body|integer| 是 ||主键ID|
|» name|body|string| 否 ||姓名|
|» password|body|string| 否 ||密码|
|» email|body|string¦null| 否 ||邮箱|
|» areaId|body|integer¦null| 否 ||所属区域ID|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": {
    "zone": {
      "id": 1,
      "name": "string",
      "polygon": "string"
    },
    "dispatcher": {
      "id": 1,
      "name": "string",
      "password": "string",
      "email": "string",
      "areaId": -2147483648,
      "createTime": "CURRENT_TIMESTAMP"
    }
  }
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||code|
|» msg|string|true|none||none|
|» data|object¦null|true|none||none|
|»» zone|object|true|none||none|
|»»» id|integer|true|none||主键ID|
|»»» name|string|true|none||区域名称|
|»»» polygon|string¦null|false|none||多边形范围坐标|
|»» dispatcher|object|true|none||none|
|»»» id|integer|true|none||主键ID|
|»»» name|string|true|none||姓名|
|»»» password|string|true|none||密码|
|»»» email|string¦null|false|none||邮箱|
|»»» areaId|integer¦null|false|none||所属区域ID|
|»»» createTime|string(date-time)¦null|false|none||创建时间|

## DELETE 删除调度员

DELETE /admin/dispatchers/deleteDispatcher

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|dispatcherId|query|integer| 是 ||none|
|name|query|string| 否 ||none|
|email|query|integer| 否 ||none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||none|
|» msg|string|true|none||none|
|» data|object¦null|true|none||none|

# 停车点接口

## GET 获取停车点列表

GET /admin/ParkingPoint/getPointList

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|page|query|integer| 否 ||none|
|pagesize|query|integer| 否 ||none|
|keyword|query|string| 否 ||none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": {
    "areaList": [
      {
        "id": 0,
        "name": "string",
        "latitude": 0,
        "longtitude": 0,
        "status": 0,
        "create_time": "string"
      }
    ],
    "page": 0,
    "pageSize": 0,
    "total": 0
  }
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||none|
|» msg|string|true|none||none|
|» data|object|true|none||none|
|»» areaList|[object]|true|none||none|
|»»» id|integer|true|none||none|
|»»» name|string¦null|true|none||none|
|»»» latitude|number|true|none||none|
|»»» longtitude|number|true|none||none|
|»»» status|integer|true|none||0禁用 1启用|
|»»» create_time|string|true|none||none|
|»» page|integer|true|none||none|
|»» pageSize|integer|true|none||none|
|»» total|integer|true|none||none|

## POST 新增停车点

POST /admin/ParkingPoint/addPoint

> Body 请求参数

```json
{
  "name": "string",
  "latitude": 0,
  "longtitude": 0,
  "status": 0
}
```

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|body|body|object| 是 ||none|
|» name|body|string¦null| 是 ||none|
|» latitude|body|number| 是 ||none|
|» longtitude|body|number| 是 ||none|
|» status|body|integer| 是 ||0禁用 1启用|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": {
    "id": 0,
    "name": "string",
    "latitude": 0,
    "longtitude": 0,
    "status": 0,
    "create_time": "string"
  }
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||none|
|» msg|string|true|none||none|
|» data|object|true|none||none|
|»» id|integer|true|none||none|
|»» name|string¦null|true|none||none|
|»» latitude|number|true|none||none|
|»» longtitude|number|true|none||none|
|»» status|integer|true|none||0禁用 1启用|
|»» create_time|string|true|none||none|

## POST 编辑停车点

POST /admin/ParkingPoint/editPoint

> Body 请求参数

```json
{
  "id": 0,
  "name": "string",
  "latitude": 0,
  "longtitude": 0,
  "status": 0,
  "create_time": "string"
}
```

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|body|body|object| 是 ||none|
|» id|body|integer| 是 ||none|
|» name|body|string¦null| 是 ||none|
|» latitude|body|number| 是 ||none|
|» longtitude|body|number| 是 ||none|
|» status|body|integer| 是 ||0禁用 1启用|
|» create_time|body|string| 是 ||none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": {
    "id": 0,
    "name": "string",
    "latitude": 0,
    "longtitude": 0,
    "status": 0,
    "create_time": "string"
  }
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||code|
|» msg|string|true|none||none|
|» data|object¦null|true|none||none|
|»» id|integer|true|none||none|
|»» name|string¦null|true|none||none|
|»» latitude|number|true|none||none|
|»» longtitude|number|true|none||none|
|»» status|integer|true|none||0禁用 1启用|
|»» create_time|string|true|none||none|

## DELETE 删除停车点

DELETE /admin/ParkingPoint/deletePoint

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|ParkingPointId|query|integer| 是 ||none|
|name|query|string| 否 ||none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||none|
|» msg|string|true|none||none|
|» data|object¦null|true|none||none|

# 小车接口

## GET 获取小车列表

GET /admin/scooter/getScooterList

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|areaId|query|integer| 否 ||none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": {
    "areaList": [
      {
        "id": 1,
        "code": "string",
        "rideStatus": "0",
        "faultStatus": 0,
        "battery": "100",
        "latitude": 0,
        "longitude": 0,
        "createTime": "CURRENT_TIMESTAMP"
      }
    ],
    "areaId": 0,
    "scooterCount": "string"
  }
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||none|
|» msg|string|true|none||none|
|» data|object|true|none||none|
|»» areaList|[object]|true|none||none|
|»»» id|integer|true|none||主键ID|
|»»» code|string|true|none||车辆编码|
|»»» rideStatus|integer¦null|false|none||状态：0-空闲，1-使用中|
|»»» faultStatus|integer|true|none||0-正常 1-故障|
|»»» battery|integer¦null|false|none||电量|
|»»» latitude|number¦null|false|none||纬度|
|»»» longitude|number¦null|false|none||经度|
|»»» createTime|string(date-time)¦null|false|none||创建时间|
|»» areaId|integer|false|none||none|
|»» scooterCount|string|true|none||none|

# 数据模型

<h2 id="tocS_Admin">Admin</h2>

<a id="schemaadmin"></a>
<a id="schema_Admin"></a>
<a id="tocSadmin"></a>
<a id="tocsadmin"></a>

```json
{
  "id": 1,
  "adminName": "string",
  "password": "string",
  "email": "string",
  "createTime": "CURRENT_TIMESTAMP"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|integer|true|none||主键ID|
|adminName|string|true|none||用户名|
|password|string|true|none||密码|
|email|string¦null|false|none||电子邮箱|
|createTime|string(date-time)¦null|false|none||创建时间|

<h2 id="tocS_ParkingPoints">ParkingPoints</h2>

<a id="schemaparkingpoints"></a>
<a id="schema_ParkingPoints"></a>
<a id="tocSparkingpoints"></a>
<a id="tocsparkingpoints"></a>

```json
{
  "id": 0,
  "name": "string",
  "latitude": 0,
  "longtitude": 0,
  "status": 0,
  "create_time": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|integer|true|none||none|
|name|string¦null|true|none||none|
|latitude|number|true|none||none|
|longtitude|number|true|none||none|
|status|integer|true|none||0禁用 1启用|
|create_time|string|true|none||none|

<h2 id="tocS_Area">Area</h2>

<a id="schemaarea"></a>
<a id="schema_Area"></a>
<a id="tocSarea"></a>
<a id="tocsarea"></a>

```json
{
  "id": 1,
  "name": "string",
  "polygon": "string",
  "createTime": "CURRENT_TIMESTAMP"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|integer|true|none||主键ID|
|name|string|true|none||区域名称|
|polygon|string¦null|false|none||多边形范围坐标|
|createTime|string(date-time)¦null|false|none||创建时间|

<h2 id="tocS_Dispatcher">Dispatcher</h2>

<a id="schemadispatcher"></a>
<a id="schema_Dispatcher"></a>
<a id="tocSdispatcher"></a>
<a id="tocsdispatcher"></a>

```json
{
  "id": 1,
  "name": "string",
  "password": "string",
  "email": "string",
  "areaId": -2147483648,
  "createTime": "CURRENT_TIMESTAMP"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|integer|true|none||主键ID|
|name|string|true|none||姓名|
|password|string|true|none||密码|
|email|string¦null|false|none||邮箱|
|areaId|integer¦null|false|none||所属区域ID|
|createTime|string(date-time)¦null|false|none||创建时间|

<h2 id="tocS_FaultReport">FaultReport</h2>

<a id="schemafaultreport"></a>
<a id="schema_FaultReport"></a>
<a id="tocSfaultreport"></a>
<a id="tocsfaultreport"></a>

```json
{
  "id": 1,
  "userId": -2147483648,
  "scooterId": -2147483648,
  "description": "string",
  "imageUrl": "string",
  "status": "0",
  "createTime": "CURRENT_TIMESTAMP"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|integer|true|none||主键ID|
|userId|integer|true|none||用户ID|
|scooterId|integer|true|none||车辆ID|
|description|string¦null|false|none||故障描述|
|imageUrl|string¦null|false|none||图片链接|
|status|integer¦null|false|none||处理状态：0-未处理，1-已处理|
|createTime|string(date-time)¦null|false|none||创建时间|

<h2 id="tocS_NoParkingArea">NoParkingArea</h2>

<a id="schemanoparkingarea"></a>
<a id="schema_NoParkingArea"></a>
<a id="tocSnoparkingarea"></a>
<a id="tocsnoparkingarea"></a>

```json
{
  "id": 1,
  "name": "string",
  "polygon": "string",
  "status": "1",
  "createTime": "CURRENT_TIMESTAMP"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|integer|true|none||主键ID|
|name|string¦null|false|none||禁停区名称|
|polygon|string¦null|false|none||禁停多边形范围坐标|
|status|integer¦null|false|none||状态：1-启用，0-禁用|
|createTime|string(date-time)¦null|false|none||创建时间|

<h2 id="tocS_PackageOrder">PackageOrder</h2>

<a id="schemapackageorder"></a>
<a id="schema_PackageOrder"></a>
<a id="tocSpackageorder"></a>
<a id="tocspackageorder"></a>

```json
{
  "id": 1,
  "userId": -2147483648,
  "packageId": -2147483648,
  "price": 0,
  "orderStatus": "0",
  "payStatus": "0",
  "createTime": "CURRENT_TIMESTAMP"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|integer|true|none||主键ID|
|userId|integer|true|none||用户ID|
|packageId|integer|true|none||套餐ID|
|price|number|true|none||购买价格|
|orderStatus|integer¦null|false|none||订单状态：0-待支付，1-已结束|
|payStatus|integer¦null|false|none||支付状态：0-未付，1-已付|
|createTime|string(date-time)¦null|false|none||创建时间|

<h2 id="tocS_PricingRule">PricingRule</h2>

<a id="schemapricingrule"></a>
<a id="schema_PricingRule"></a>
<a id="tocSpricingrule"></a>
<a id="tocspricingrule"></a>

```json
{
  "id": 1,
  "pricePerMin": "0.00",
  "basePrice": "0.00",
  "billingInterval": "30",
  "createTime": "CURRENT_TIMESTAMP"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|integer|true|none||主键ID|
|pricePerMin|number¦null|false|none||每分钟单价|
|basePrice|number¦null|false|none||起步价|
|billingInterval|integer¦null|false|none||计费间隔（分钟）|
|createTime|string(date-time)¦null|false|none||创建时间|

<h2 id="tocS_Scooter">Scooter</h2>

<a id="schemascooter"></a>
<a id="schema_Scooter"></a>
<a id="tocSscooter"></a>
<a id="tocsscooter"></a>

```json
{
  "id": 1,
  "code": "string",
  "ride_status": "0",
  "fault_status": 0,
  "battery": "100",
  "latitude": 0,
  "longitude": 0,
  "createTime": "CURRENT_TIMESTAMP"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|integer|true|none||主键ID|
|code|string|true|none||车辆编码|
|ride_status|integer¦null|false|none||状态：0-空闲，1-使用中|
|fault_status|integer|true|none||0-正常 1-故障|
|battery|integer¦null|false|none||电量|
|latitude|number¦null|false|none||纬度|
|longitude|number¦null|false|none||经度|
|createTime|string(date-time)¦null|false|none||创建时间|

<h2 id="tocS_RentalOrder">RentalOrder</h2>

<a id="schemarentalorder"></a>
<a id="schema_RentalOrder"></a>
<a id="tocSrentalorder"></a>
<a id="tocsrentalorder"></a>

```json
{
  "id": 1,
  "userId": -2147483648,
  "scooterId": -2147483648,
  "startTime": "2019-08-24T14:15:22Z",
  "endTime": "2019-08-24T14:15:22Z",
  "totalTime": "0",
  "orderStatus": "0",
  "payStatus": "0",
  "amount": "0.00",
  "totalKilometer": "0.00",
  "createTime": "CURRENT_TIMESTAMP"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|integer|true|none||主键ID|
|userId|integer|true|none||用户ID|
|scooterId|integer|true|none||车辆ID|
|startTime|string(date-time)¦null|false|none||开始时间|
|endTime|string(date-time)¦null|false|none||结束时间|
|totalTime|integer¦null|false|none||骑行总时长（单位：分钟）|
|orderStatus|integer¦null|false|none||订单状态：0-骑行中，1-待支付，2-已结束|
|payStatus|integer¦null|false|none||支付状态：0-未支付，1-已支付|
|amount|number¦null|false|none||金额|
|totalKilometer|number¦null|false|none||总里程|
|createTime|string(date-time)¦null|false|none||创建时间|

<h2 id="tocS_SubscriptionPackage">SubscriptionPackage</h2>

<a id="schemasubscriptionpackage"></a>
<a id="schema_SubscriptionPackage"></a>
<a id="tocSsubscriptionpackage"></a>
<a id="tocssubscriptionpackage"></a>

```json
{
  "id": 1,
  "title": "string",
  "description": "string",
  "price": 0,
  "type": "1",
  "createTime": "CURRENT_TIMESTAMP"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|integer|true|none||主键ID|
|title|string|true|none||套餐标题|
|description|string¦null|false|none||套餐描述|
|price|number|true|none||价格|
|type|integer¦null|false|none||类型：1-月卡，2-季卡，3-年卡|
|createTime|string(date-time)¦null|false|none||创建时间|

<h2 id="tocS_User">User</h2>

<a id="schemauser"></a>
<a id="schema_User"></a>
<a id="tocSuser"></a>
<a id="tocsuser"></a>

```json
{
  "id": 1,
  "username": "string",
  "password": "string",
  "email": "string",
  "createTime": "CURRENT_TIMESTAMP"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|integer|true|none||主键ID|
|username|string|true|none||用户名|
|password|string|true|none||密码|
|email|string¦null|false|none||电子邮箱|
|createTime|string(date-time)¦null|false|none||创建时间|

<h2 id="tocS_UserBill">UserBill</h2>

<a id="schemauserbill"></a>
<a id="schema_UserBill"></a>
<a id="tocSuserbill"></a>
<a id="tocsuserbill"></a>

```json
{
  "id": 1,
  "userId": -2147483648,
  "type": -2147483648,
  "amount": 0,
  "balanceAfter": 0,
  "orderId": -2147483648,
  "remark": "string",
  "createTime": "CURRENT_TIMESTAMP"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|integer|true|none||主键ID|
|userId|integer|true|none||用户ID|
|type|integer|true|none||流水类型：1-骑行消费，2-充值，3-退款，4-购买套餐|
|amount|number|true|none||变动金额（正数为进账，负数为支出）|
|balanceAfter|number|true|none||变动后账户余额（用于对账）|
|orderId|integer¦null|false|none||业务订单ID|
|remark|string¦null|false|none||流水备注（如：骑行订单1002扣费）|
|createTime|string(date-time)¦null|false|none||创建时间|

<h2 id="tocS_UserWallet">UserWallet</h2>

<a id="schemauserwallet"></a>
<a id="schema_UserWallet"></a>
<a id="tocSuserwallet"></a>
<a id="tocsuserwallet"></a>

```json
{
  "id": 1,
  "userId": -2147483648,
  "balance": "0.00",
  "updateTime": "CURRENT_TIMESTAMP"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|integer|true|none||主键ID|
|userId|integer|true|none||用户ID|
|balance|number¦null|false|none||余额|
|updateTime|string(date-time)¦null|false|none||更新时间|

