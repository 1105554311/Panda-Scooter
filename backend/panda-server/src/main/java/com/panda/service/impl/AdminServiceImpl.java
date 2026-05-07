package com.panda.service.impl;

import com.panda.context.BaseContext;
import com.panda.dto.*;
import com.panda.entity.*;
import com.panda.exception.BaseException;
import com.panda.mapper.*;
import com.panda.properties.JwtProperties;
import com.panda.service.AdminService;
import com.panda.vo.*;
import com.panda.utils.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AdminServiceImpl implements AdminService {

    @Autowired
    private AdminMapper adminMapper;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private RentalOrderMapper rentalOrderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ScooterMapper scooterMapper;

    @Autowired
    private UserBillMapper userBillMapper;

    @Autowired
    private PricingRuleMapper pricingRuleMapper;

    @Autowired
    private SubscriptionPackageMapper subscriptionPackageMapper;

    @Autowired
    private AreaMapper areaMapper;

    @Autowired
    private DispatcherMapper dispatcherMapper;

    @Override
    public AdminLoginVO login(AdminLoginDTO adminLoginDTO) {
        Admin admin = adminMapper.getByEmail(adminLoginDTO.getEmail());
        if (admin == null) {
            throw new BaseException("账号不存在");
        }
        if (!admin.getPassword().equals(encrypt(adminLoginDTO.getPassword()))) {
            throw new BaseException("密码错误");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("adminId", admin.getId());
        String token = JwtUtil.createJWT(jwtProperties.getSecretKey(), jwtProperties.getTtl(), claims);

        return AdminLoginVO.builder()
                .id(admin.getId())
                .username(admin.getUsername())
                .email(admin.getEmail())
                .token(token)
                .build();
    }

    @Override
    public void logout() {
        Long adminId = BaseContext.getCurrentId();
        if (adminId == null) {
            throw new BaseException("未登录");
        }
        log.info("管理员登出，adminId: {}", adminId);
        BaseContext.removeCurrentId();
    }

    @Override
    public DataOverviewVO getOverview(DataOverviewDTO overviewDTO) {
        if (overviewDTO.getStartDate() == null) {
            overviewDTO.setStartDate(LocalDate.now().minusDays(30));
        }
        if (overviewDTO.getEndDate() == null) {
            overviewDTO.setEndDate(LocalDate.now());
        }

        List<Map<String, Object>> trendResults = rentalOrderMapper.getTrendData(
                overviewDTO.getStartDate(),
                overviewDTO.getEndDate(),
                overviewDTO.getGranularity(),
                overviewDTO.getAreaId()
        );

        List<DataOverviewVO.SeriesItem> series = new ArrayList<>();
        if (trendResults != null) {
            for (Map<String, Object> row : trendResults) {
                series.add(DataOverviewVO.SeriesItem.builder()
                        .time(row.get("date").toString())
                        .orderCount(String.valueOf(((Number) row.get("order_count")).intValue()))
                        .revenue(row.get("revenue") != null ? row.get("revenue").toString() : "0")
                        .build());
            }
        }

        return DataOverviewVO.builder()
                .startDate(overviewDTO.getStartDate().toString())
                .endDate(overviewDTO.getEndDate().toString())
                .granularity(overviewDTO.getGranularity() != null ? overviewDTO.getGranularity() : "day")
                .areaID(overviewDTO.getAreaId() != null ? overviewDTO.getAreaId().toString() : null)
                .series(series)
                .build();
    }

    @Override
    public LiveDataVO getLiveData(LiveDataDTO liveDataDTO) {
        LocalDate targetDate = liveDataDTO.getDate();
        if (targetDate == null) {
            targetDate = LocalDate.now();
        }

        Integer areaId = liveDataDTO.getAreaId();

        Integer todayOrders = rentalOrderMapper.countByDateRange(targetDate, targetDate, areaId);
        BigDecimal todayRevenue = userBillMapper.sumAmountByDate(targetDate, areaId);
        Integer onlineScooters = scooterMapper.countByStatus(0);
        Integer faultScooters = scooterMapper.countByStatus(2);

        return LiveDataVO.builder()
                .updatedAt(java.time.LocalDateTime.now())
                .todayOrders(todayOrders != null ? todayOrders.toString() : "0")
                .todayRevenue(todayRevenue != null ? todayRevenue.toString() : "0")
                .onlineScooters(onlineScooters != null ? onlineScooters.toString() : "0")
                .faultScooters(faultScooters != null ? faultScooters.toString() : "0")
                .areaId(areaId != null ? areaId.toString() : null)
                .build();
    }

    @Override
    public PricingRuleVO getPricingRules() {
        List<PricingRule> rules = pricingRuleMapper.getAllRules();
        if (rules == null || rules.isEmpty()) {
            return PricingRuleVO.builder().build();
        }
        PricingRule rule = rules.get(0);
        return PricingRuleVO.builder()
                .id(rule.getId())
                .pricePerMin(rule.getPricePerMin())
                .basePrice(rule.getBasePrice())
                .billingInterval(rule.getBillingInterval())
                .build();
    }

    @Override
    public void editRules(PricingRuleEditDTO editDTO) {
        if (editDTO.getPricePerMin() == null && editDTO.getBasePrice() == null
                && editDTO.getBillingInterval() == null) {
            throw new BaseException("没有需要修改的数据");
        }
        int rows = pricingRuleMapper.updateRule(editDTO);
        if (rows == 0) {
            pricingRuleMapper.insertRule(editDTO);
        }
    }

    @Override
    public PackageListVO getPackageList(PackageListDTO packageListDTO) {
        log.info("获取套餐列表，参数：{}", packageListDTO);

        // 设置默认值
        int page = packageListDTO.getPage() != null ? packageListDTO.getPage() : 1;
        int pageSize = packageListDTO.getPageSize() != null ? packageListDTO.getPageSize() : 10;
        int offset = (page - 1) * pageSize;

        String status = packageListDTO.getStatus();
        String keyword = packageListDTO.getKeyword();

        // 查询数据
        List<SubscriptionPackage> packageList = subscriptionPackageMapper.getPackageList(offset, pageSize, status, keyword);
        Integer total = subscriptionPackageMapper.countPackageList(status, keyword);

        // 转换为 VO
        List<PackageListVO.PackageItem> items = new ArrayList<>();
        if (packageList != null) {
            for (SubscriptionPackage pkg : packageList) {
                items.add(PackageListVO.PackageItem.builder()
                        .id(pkg.getId())
                        .title(pkg.getTitle())
                        .description(pkg.getDescription())
                        .type(pkg.getType())
                        .price(pkg.getPrice())
                        .status(pkg.getStatus())
                        .createTime(pkg.getCreateTime() != null ? pkg.getCreateTime().toString() : null)
                        .build());
            }
        }

        return PackageListVO.builder()
                .total(total != null ? total : 0)
                .page(page)
                .pageSize(pageSize)
                .list(items)
                .build();
    }

    @Override
    public void addPackage(AddPackageDTO addPackageDTO) {
        log.info("新增套餐，参数：{}", addPackageDTO);

        // 参数校验
        if (addPackageDTO.getTitle() == null || addPackageDTO.getTitle().isEmpty()) {
            throw new BaseException("套餐标题不能为空");
        }
        if (addPackageDTO.getPrice() == null) {
            throw new BaseException("套餐价格不能为空");
        }
        if (addPackageDTO.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BaseException("套餐价格必须大于0");
        }

        // 构建实体类
        SubscriptionPackage subscriptionPackage = new SubscriptionPackage();
        subscriptionPackage.setTitle(addPackageDTO.getTitle());
        subscriptionPackage.setDescription(addPackageDTO.getDescription());
        subscriptionPackage.setPrice(addPackageDTO.getPrice());
        subscriptionPackage.setType(addPackageDTO.getType() != null ? addPackageDTO.getType() : 1);
        subscriptionPackage.setStatus(1);  // 默认启用

        // 处理创建时间
        if (addPackageDTO.getCreateTime() != null) {
            // 如果有传入，解析时间字符串
            try {
                subscriptionPackage.setCreateTime(LocalDateTime.parse(addPackageDTO.getCreateTime(),
                        java.time.format.DateTimeFormatter.ISO_DATE_TIME));
            } catch (Exception e) {
                subscriptionPackage.setCreateTime(LocalDateTime.now());
            }
        } else {
            subscriptionPackage.setCreateTime(LocalDateTime.now());
        }

        // 插入数据库
        int rows = subscriptionPackageMapper.insertPackage(subscriptionPackage);
        if (rows == 0) {
            throw new BaseException("新增套餐失败");
        }

        log.info("新增套餐成功，id：{}", subscriptionPackage.getId());
    }

    @Override
    public void editPackage(EditPackageDTO editPackageDTO) {
        log.info("编辑套餐，参数：{}", editPackageDTO);

        // 参数校验
        if (editPackageDTO.getId() == null) {
            throw new BaseException("套餐ID不能为空");
        }
        if (editPackageDTO.getPrice() == null) {
            throw new BaseException("套餐价格不能为空");
        }
        if (editPackageDTO.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BaseException("套餐价格必须大于0");
        }

        // 检查套餐是否存在
        SubscriptionPackage existingPackage = subscriptionPackageMapper.getById(editPackageDTO.getId());
        if (existingPackage == null) {
            throw new BaseException("套餐不存在");
        }

        // 更新字段（只更新非空的字段）
        SubscriptionPackage updatePackage = new SubscriptionPackage();
        updatePackage.setId(editPackageDTO.getId());

        if (editPackageDTO.getTitle() != null && !editPackageDTO.getTitle().isEmpty()) {
            updatePackage.setTitle(editPackageDTO.getTitle());
        }
        if (editPackageDTO.getDescription() != null) {
            updatePackage.setDescription(editPackageDTO.getDescription());
        }
        updatePackage.setPrice(editPackageDTO.getPrice());
        if (editPackageDTO.getType() != null) {
            updatePackage.setType(editPackageDTO.getType());
        }

        // 更新数据库
        int rows = subscriptionPackageMapper.updatePackage(updatePackage);
        if (rows == 0) {
            throw new BaseException("编辑套餐失败");
        }

        log.info("编辑套餐成功，id：{}", editPackageDTO.getId());
    }

    @Override
    public void deletePackage(DeletePackageDTO deletePackageDTO) {
        log.info("删除套餐，参数：{}", deletePackageDTO);

        // 参数校验
        if (deletePackageDTO.getId() == null) {
            throw new BaseException("套餐ID不能为空");
        }

        // 检查套餐是否存在
        SubscriptionPackage existingPackage = subscriptionPackageMapper.getById(deletePackageDTO.getId());
        if (existingPackage == null) {
            throw new BaseException("套餐不存在");
        }

        // 删除套餐
        int rows = subscriptionPackageMapper.deleteById(deletePackageDTO.getId());
        if (rows == 0) {
            throw new BaseException("删除套餐失败");
        }

        log.info("删除套餐成功，id：{}", deletePackageDTO.getId());
    }

    @Override
    public ZoneListVO getZoneList(ZoneListDTO zoneListDTO) {
        log.info("获取片区列表，参数：{}", zoneListDTO);

        // 设置默认值
        int page = zoneListDTO.getPage() != null ? zoneListDTO.getPage() : 1;
        int pageSize = zoneListDTO.getPageSize() != null ? zoneListDTO.getPageSize() : 10;
        int offset = (page - 1) * pageSize;

        String keyword = zoneListDTO.getKeyword();

        // 查询数据
        List<Area> areaList = areaMapper.getZoneList(offset, pageSize, keyword);
        Integer total = areaMapper.countZoneList(keyword);

        // 转换为 VO
        List<ZoneListVO.ZoneItem> items = new ArrayList<>();
        if (areaList != null) {
            for (Area area : areaList) {
                items.add(ZoneListVO.ZoneItem.builder()
                        .id(area.getId())
                        .name(area.getName())
                        .polygon(area.getPolygon())
                        .createTime(area.getCreateTime())
                        .build());
            }
        }

        return ZoneListVO.builder()
                .areaList(items)
                .page(page)
                .pageSize(pageSize)
                .total(total != null ? total : 0)
                .build();
    }

    @Override
    public AddZoneVO addZone(AddZoneDTO addZoneDTO) {
        log.info("新增片区，参数：{}", addZoneDTO);

        // 参数校验
        if (addZoneDTO.getName() == null || addZoneDTO.getName().isEmpty()) {
            throw new BaseException("区域名称不能为空");
        }

        // 构建实体类
        Area area = new Area();
        area.setName(addZoneDTO.getName());
        area.setPolygon(addZoneDTO.getPolygon());
        area.setCreateTime(LocalDateTime.now());

        // 插入数据库
        int rows = areaMapper.insertArea(area);
        if (rows == 0) {
            throw new BaseException("新增片区失败");
        }

        // 计算多边形点数（如果 polygon 是 JSON 数组格式）
        Integer polygonPointCount = 0;
        if (addZoneDTO.getPolygon() != null && !addZoneDTO.getPolygon().isEmpty()) {
            // 简单计算点数，根据实际格式调整
            polygonPointCount = countPolygonPoints(addZoneDTO.getPolygon());
        }

        // 获取当前登录用户
        Long adminId = BaseContext.getCurrentId();
        String createdBy = adminId != null ? adminId.toString() : "admin";

        return AddZoneVO.builder()
                .id(area.getId())
                .name(area.getName())
                .polygon(area.getPolygon())
                .dispatcherId(addZoneDTO.getDispatcherId())
                .polygonPointCount(polygonPointCount)
                .createdBy(createdBy)
                .build();
    }

    /**
     * 计算多边形点数
     */
    private Integer countPolygonPoints(String polygon) {
        if (polygon == null || polygon.isEmpty()) {
            return 0;
        }
        try {
            // 假设 polygon 是 JSON 数组格式，如 [[lng,lat],...]
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            List<List<Double>> points = mapper.readValue(polygon,
                    new com.fasterxml.jackson.core.type.TypeReference<List<List<Double>>>() {});
            return points != null ? points.size() : 0;
        } catch (Exception e) {
            log.warn("解析多边形点数失败", e);
            return 0;
        }
    }

    @Override
    public ZoneDetailVO getZoneDetail(ZoneDetailDTO zoneDetailDTO) {
        log.info("获取片区详情，参数：{}", zoneDetailDTO);

        if (zoneDetailDTO.getId() == null) {
            throw new BaseException("片区ID不能为空");
        }

        Area area = areaMapper.getZoneDetailById(zoneDetailDTO.getId());
        if (area == null) {
            throw new BaseException("片区不存在");
        }

        return ZoneDetailVO.builder()
                .id(area.getId())
                .name(area.getName())
                .polygon(area.getPolygon())
                .build();
    }

    @Override
    public EditZoneVO editZone(EditZoneDTO editZoneDTO) {
        log.info("编辑片区，参数：{}", editZoneDTO);

        if (editZoneDTO.getId() == null) {
            throw new BaseException("片区ID不能为空");
        }

        // 检查片区是否存在
        Area existingArea = areaMapper.getZoneDetailById(editZoneDTO.getId());
        if (existingArea == null) {
            throw new BaseException("片区不存在");
        }

        // 更新片区
        Area updateArea = new Area();
        updateArea.setId(editZoneDTO.getId());

        if (editZoneDTO.getName() != null && !editZoneDTO.getName().isEmpty()) {
            updateArea.setName(editZoneDTO.getName());
        }
        if (editZoneDTO.getPolygon() != null) {
            updateArea.setPolygon(editZoneDTO.getPolygon());
        }

        int rows = areaMapper.updateArea(updateArea);
        if (rows == 0) {
            throw new BaseException("编辑片区失败");
        }

        // 获取更新后的片区信息
        Area updatedArea = areaMapper.getZoneDetailById(editZoneDTO.getId());

        // 构建 zone
        EditZoneVO.ZoneInfo zoneInfo = EditZoneVO.ZoneInfo.builder()
                .id(updatedArea.getId())
                .name(updatedArea.getName())
                .polygon(updatedArea.getPolygon())
                .createTime(updatedArea.getCreateTime() != null ?
                        updatedArea.getCreateTime().toString() : null)
                .build();

        // 查询该片区下的调度员（取第一个）
        EditZoneVO.DispatcherInfo dispatcherInfo = null;
        Dispatcher dispatcher = dispatcherMapper.getByAreaId(updatedArea.getId());
        if (dispatcher != null) {
            dispatcherInfo = EditZoneVO.DispatcherInfo.builder()
                    .id(dispatcher.getId())
                    .name(dispatcher.getName())
                    .email(dispatcher.getEmail())
                    .areaId(dispatcher.getAreaId())
                    .build();
        }

        return EditZoneVO.builder()
                .zone(zoneInfo)
                .dispatcher(dispatcherInfo)
                .build();
    }

    @Override
    public void deleteZone(DeleteZoneDTO deleteZoneDTO) {
        log.info("删除片区，参数：{}", deleteZoneDTO);

        // 参数校验
        if (deleteZoneDTO.getId() == null) {
            throw new BaseException("片区ID不能为空");
        }

        // 检查片区是否存在
        Area existingArea = areaMapper.getZoneDetailById(deleteZoneDTO.getId());
        if (existingArea == null) {
            throw new BaseException("片区不存在");
        }

        // 如果传了 name，进行二次校验
        if (deleteZoneDTO.getName() != null && !deleteZoneDTO.getName().isEmpty()) {
            if (!existingArea.getName().equals(deleteZoneDTO.getName())) {
                throw new BaseException("片区名称不匹配，删除失败");
            }
        }

        // 删除片区
        int rows = areaMapper.deleteById(deleteZoneDTO.getId());
        if (rows == 0) {
            throw new BaseException("删除片区失败");
        }

        log.info("删除片区成功，id：{}", deleteZoneDTO.getId());
    }

    @Override
    public DispatcherListVO getDispatcherList(DispatcherListDTO dispatcherListDTO) {
        log.info("获取调度员列表，参数：{}", dispatcherListDTO);

        // 设置默认值
        int page = dispatcherListDTO.getPage() != null ? dispatcherListDTO.getPage() : 1;
        int pageSize = dispatcherListDTO.getPageSize() != null ? dispatcherListDTO.getPageSize() : 10;
        int offset = (page - 1) * pageSize;

        String keyword = dispatcherListDTO.getKeyword();
        Long areaId = dispatcherListDTO.getAreaId();

        // 查询数据
        List<Dispatcher> dispatcherList = dispatcherMapper.getDispatcherList(offset, pageSize, keyword, areaId);
        Integer total = dispatcherMapper.countDispatcherList(keyword, areaId);

        // 转换为 VO
        List<DispatcherListVO.DispatcherItem> items = new ArrayList<>();
        if (dispatcherList != null) {
            for (Dispatcher dispatcher : dispatcherList) {
                items.add(DispatcherListVO.DispatcherItem.builder()
                        .id(dispatcher.getId())
                        .name(dispatcher.getName())
                        .email(dispatcher.getEmail())
                        .areaId(dispatcher.getAreaId())
                        .createTime(dispatcher.getCreateTime())
                        .build());
            }
        }

        return DispatcherListVO.builder()
                .dispatcherList(items)
                .page(page)
                .pageSize(pageSize)
                .total(total != null ? total : 0)
                .build();
    }

    @Override
    public AddDispatcherVO addDispatcher(AddDispatcherDTO addDispatcherDTO) {
        log.info("新增调度员，参数：{}", addDispatcherDTO);

        // 参数校验
        if (addDispatcherDTO.getName() == null || addDispatcherDTO.getName().isEmpty()) {
            throw new BaseException("姓名不能为空");
        }
        if (addDispatcherDTO.getPassword() == null || addDispatcherDTO.getPassword().isEmpty()) {
            throw new BaseException("密码不能为空");
        }
        if (addDispatcherDTO.getEmail() == null || addDispatcherDTO.getEmail().isEmpty()) {
            throw new BaseException("邮箱不能为空");
        }

        // 检查邮箱是否已存在
        Dispatcher existingDispatcher = dispatcherMapper.getByEmail(addDispatcherDTO.getEmail());
        if (existingDispatcher != null) {
            throw new BaseException("邮箱已存在");
        }

        // 构建实体类
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setName(addDispatcherDTO.getName());
        dispatcher.setPassword(encrypt(addDispatcherDTO.getPassword()));  // 密码加密
        dispatcher.setEmail(addDispatcherDTO.getEmail());
        dispatcher.setAreaId(addDispatcherDTO.getAreaId());
        dispatcher.setStatus(1);  // 默认启用
        dispatcher.setCreateTime(LocalDateTime.now());

        // 插入数据库
        int rows = dispatcherMapper.insertDispatcher(dispatcher);
        if (rows == 0) {
            throw new BaseException("新增调度员失败");
        }

        log.info("新增调度员成功，id：{}", dispatcher.getId());

        return AddDispatcherVO.builder()
                .id(dispatcher.getId())
                .name(dispatcher.getName())
                .email(dispatcher.getEmail())
                .areaId(dispatcher.getAreaId())
                .createTime(dispatcher.getCreateTime())
                .build();
    }

    @Override
    public EditDispatcherVO editDispatcher(EditDispatcherDTO editDispatcherDTO) {
        log.info("编辑调度员，参数：{}", editDispatcherDTO);

        // 参数校验
        if (editDispatcherDTO.getId() == null) {
            throw new BaseException("调度员ID不能为空");
        }

        // 检查调度员是否存在
        Dispatcher existingDispatcher = dispatcherMapper.getById(editDispatcherDTO.getId());
        if (existingDispatcher == null) {
            throw new BaseException("调度员不存在");
        }

        // 如果修改邮箱，检查新邮箱是否已被其他调度员使用
        if (editDispatcherDTO.getEmail() != null && !editDispatcherDTO.getEmail().isEmpty()) {
            Dispatcher dispatcherByEmail = dispatcherMapper.getByEmail(editDispatcherDTO.getEmail());
            if (dispatcherByEmail != null && !dispatcherByEmail.getId().equals(editDispatcherDTO.getId())) {
                throw new BaseException("邮箱已被其他调度员使用");
            }
        }

        // 构建更新对象
        Dispatcher updateDispatcher = new Dispatcher();
        updateDispatcher.setId(editDispatcherDTO.getId());

        if (editDispatcherDTO.getName() != null && !editDispatcherDTO.getName().isEmpty()) {
            updateDispatcher.setName(editDispatcherDTO.getName());
        }
        if (editDispatcherDTO.getPassword() != null && !editDispatcherDTO.getPassword().isEmpty()) {
            updateDispatcher.setPassword(encrypt(editDispatcherDTO.getPassword())); // 密码加密
        }
        if (editDispatcherDTO.getEmail() != null && !editDispatcherDTO.getEmail().isEmpty()) {
            updateDispatcher.setEmail(editDispatcherDTO.getEmail());
        }
        if (editDispatcherDTO.getAreaId() != null) {
            updateDispatcher.setAreaId(editDispatcherDTO.getAreaId());
        }

        // 更新数据库
        int rows = dispatcherMapper.updateDispatcher(updateDispatcher);
        if (rows == 0) {
            throw new BaseException("编辑调度员失败");
        }

        // 获取更新后的调度员信息
        Dispatcher updatedDispatcher = dispatcherMapper.getById(editDispatcherDTO.getId());

        return EditDispatcherVO.builder()
                .dispatcher(EditDispatcherVO.DispatcherInfo.builder()
                        .id(updatedDispatcher.getId())
                        .name(updatedDispatcher.getName())
                        .email(updatedDispatcher.getEmail())
                        .areaId(updatedDispatcher.getAreaId())
                        .status(updatedDispatcher.getStatus())
                        .createTime(updatedDispatcher.getCreateTime())
                        .build())
                .build();
    }

    private String encrypt(String value) {
        return DigestUtils.md5DigestAsHex(value.getBytes(StandardCharsets.UTF_8));
    }
}