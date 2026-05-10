package com.panda.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    @Autowired
    private NoParkingAreaMapper noParkingAreaMapper;

    @Autowired
    private ParkingPointMapper parkingPointMapper;

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
    }

    @Override
    public DataOverviewVO getOverview(DataOverviewDTO overviewDTO) {
        if (overviewDTO.getStartDate() == null) {
            overviewDTO.setStartDate(LocalDate.now().minusDays(30));
        }
        if (overviewDTO.getEndDate() == null) {
            overviewDTO.setEndDate(LocalDate.now());
        }
        String granularity = normalizeGranularity(overviewDTO.getGranularity());
        List<Long> scooterIds = resolveAreaScooterIds(overviewDTO.getAreaId());

        List<Map<String, Object>> trendResults = rentalOrderMapper.getTrendData(
                overviewDTO.getStartDate(),
                overviewDTO.getEndDate(),
                granularity,
                scooterIds
        );

        List<DataOverviewVO.SeriesItem> series = buildOverviewSeries(
                overviewDTO.getStartDate(),
                overviewDTO.getEndDate(),
                granularity,
                trendResults
        );

        return DataOverviewVO.builder()
                .startDate(overviewDTO.getStartDate().toString())
                .endDate(overviewDTO.getEndDate().toString())
                .granularity(granularity)
                .areaId(overviewDTO.getAreaId() != null ? overviewDTO.getAreaId().toString() : null)
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
        List<Long> scooterIds = resolveAreaScooterIds(areaId);

        Integer todayOrders = rentalOrderMapper.countByDateRange(targetDate, targetDate, scooterIds);
        BigDecimal todayRevenue = rentalOrderMapper.sumAmountByDateRange(targetDate, targetDate, scooterIds);
        Integer onlineScooters = scooterMapper.countByStatus(0, scooterIds);
        Integer faultScooters = scooterMapper.countByStatus(1, scooterIds);

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
        int pageSize = packageListDTO.getPagesize() != null ? packageListDTO.getPagesize() : 10;
        int offset = (page - 1) * pageSize;

        String keyword = packageListDTO.getKeyword();

        // 查询数据
        List<SubscriptionPackage> packageList = subscriptionPackageMapper.getPackageList(offset, pageSize, keyword);
        Integer total = subscriptionPackageMapper.countPackageList(keyword);

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
                        .createTime(pkg.getCreateTime() != null ? pkg.getCreateTime().toString() : null)
                        .build());
            }
        }

        return PackageListVO.builder()
                .total(total != null ? total : 0)
                .page(page)
                .pagesize(pageSize)
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
        int pageSize = zoneListDTO.getPagesize() != null ? zoneListDTO.getPagesize() : 10;
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
                        .dispatchers(buildZoneListDispatchers(area.getId()))
                        .build());
            }
        }

        return ZoneListVO.builder()
                .areaList(items)
                .page(page)
                .pagesize(pageSize)
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
        if (addZoneDTO.getDispatcherId() != null) {
            Dispatcher dispatcher = dispatcherMapper.getById(addZoneDTO.getDispatcherId().longValue());
            if (dispatcher == null) {
                throw new BaseException("dispatcher not found");
            }
            Dispatcher updateDispatcher = new Dispatcher();
            updateDispatcher.setId(dispatcher.getId());
            updateDispatcher.setAreaId(area.getId());
            dispatcherMapper.updateDispatcher(updateDispatcher);
        }

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
                .dispatchers(buildAddZoneDispatchers(area.getId()))
                .polygonPointCount(polygonPointCount)
                .createdBy(createdBy)
                .build();
    }

    /**
     * 计算多边形点数
     */
    private List<ZoneListVO.DispatcherInfo> buildZoneListDispatchers(Long areaId) {
        List<ZoneListVO.DispatcherInfo> result = new ArrayList<>();
        List<Dispatcher> dispatchers = dispatcherMapper.listByAreaId(areaId);
        if (dispatchers != null) {
            for (Dispatcher dispatcher : dispatchers) {
                result.add(ZoneListVO.DispatcherInfo.builder()
                        .id(dispatcher.getId())
                        .name(dispatcher.getName())
                        .build());
            }
        }
        return result;
    }

    private List<AddZoneVO.DispatcherInfo> buildAddZoneDispatchers(Long areaId) {
        List<AddZoneVO.DispatcherInfo> result = new ArrayList<>();
        List<Dispatcher> dispatchers = dispatcherMapper.listByAreaId(areaId);
        if (dispatchers != null) {
            for (Dispatcher dispatcher : dispatchers) {
                result.add(AddZoneVO.DispatcherInfo.builder()
                        .id(dispatcher.getId())
                        .name(dispatcher.getName())
                        .build());
            }
        }
        return result;
    }

    private List<ZoneDetailVO.DispatcherInfo> buildZoneDetailDispatchers(Long areaId) {
        List<ZoneDetailVO.DispatcherInfo> result = new ArrayList<>();
        List<Dispatcher> dispatchers = dispatcherMapper.listByAreaId(areaId);
        if (dispatchers != null) {
            for (Dispatcher dispatcher : dispatchers) {
                result.add(ZoneDetailVO.DispatcherInfo.builder()
                        .id(dispatcher.getId())
                        .name(dispatcher.getName())
                        .email(dispatcher.getEmail())
                        .areaId(dispatcher.getAreaId())
                        .build());
            }
        }
        return result;
    }

    private List<EditZoneVO.DispatcherInfo> buildEditZoneDispatchers(Long areaId) {
        List<EditZoneVO.DispatcherInfo> result = new ArrayList<>();
        List<Dispatcher> dispatchers = dispatcherMapper.listByAreaId(areaId);
        if (dispatchers != null) {
            for (Dispatcher dispatcher : dispatchers) {
                result.add(EditZoneVO.DispatcherInfo.builder()
                        .id(dispatcher.getId())
                        .name(dispatcher.getName())
                        .email(dispatcher.getEmail())
                        .areaId(dispatcher.getAreaId())
                        .build());
            }
        }
        return result;
    }

    private Integer countScootersInArea(String polygonText) {
        List<List<Double>> polygon = parsePolygon(polygonText);
        if (polygon.size() < 3) {
            return 0;
        }
        return findScootersInPolygon(scooterMapper.listLocated(), polygon).size();
    }

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

    private String normalizeGranularity(String granularity) {
        if (granularity == null || granularity.isBlank()) {
            return "day";
        }
        String normalized = granularity.trim().toLowerCase();
        if (!"day".equals(normalized) && !"week".equals(normalized) && !"month".equals(normalized)) {
            throw new BaseException("granularity must be day, week or month");
        }
        return normalized;
    }

    private List<DataOverviewVO.SeriesItem> buildOverviewSeries(LocalDate startDate,
                                                                LocalDate endDate,
                                                                String granularity,
                                                                List<Map<String, Object>> trendResults) {
        if (startDate.isAfter(endDate)) {
            throw new BaseException("startDate must be before or equal to endDate");
        }

        List<String> bucketTimes = new ArrayList<>();
        Map<String, Integer> orderCountByTime = new HashMap<>();
        Map<String, BigDecimal> revenueByTime = new HashMap<>();
        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            String time = cursor.toString();
            bucketTimes.add(time);
            orderCountByTime.put(time, 0);
            revenueByTime.put(time, BigDecimal.ZERO);
            cursor = nextOverviewBucket(cursor, granularity);
        }

        if (trendResults != null) {
            for (Map<String, Object> row : trendResults) {
                Object dateValue = row.get("date");
                if (dateValue == null) {
                    continue;
                }
                String bucketTime = resolveOverviewBucket(LocalDate.parse(dateValue.toString()), startDate, endDate, granularity);
                if (bucketTime == null) {
                    continue;
                }
                int orderCount = row.get("order_count") != null ? ((Number) row.get("order_count")).intValue() : 0;
                BigDecimal revenue = row.get("revenue") != null ? new BigDecimal(row.get("revenue").toString()) : BigDecimal.ZERO;
                orderCountByTime.put(bucketTime, orderCountByTime.get(bucketTime) + orderCount);
                revenueByTime.put(bucketTime, revenueByTime.get(bucketTime).add(revenue));
            }
        }

        List<DataOverviewVO.SeriesItem> series = new ArrayList<>();
        for (String time : bucketTimes) {
            series.add(DataOverviewVO.SeriesItem.builder()
                    .time(time)
                    .orderCount(String.valueOf(orderCountByTime.get(time)))
                    .revenue(revenueByTime.get(time).toString())
                    .build());
        }
        return series;
    }

    private LocalDate nextOverviewBucket(LocalDate current, String granularity) {
        if ("week".equals(granularity)) {
            return current.plusWeeks(1);
        }
        if ("month".equals(granularity)) {
            return current.plusMonths(1);
        }
        return current.plusDays(1);
    }

    private String resolveOverviewBucket(LocalDate date, LocalDate startDate, LocalDate endDate, String granularity) {
        if (date.isBefore(startDate) || date.isAfter(endDate)) {
            return null;
        }
        LocalDate bucketStart = startDate;
        if ("day".equals(granularity)) {
            bucketStart = date;
        } else if ("week".equals(granularity)) {
            long days = date.toEpochDay() - startDate.toEpochDay();
            bucketStart = startDate.plusWeeks(days / 7);
        } else if ("month".equals(granularity)) {
            while (!nextOverviewBucket(bucketStart, granularity).isAfter(date)) {
                bucketStart = nextOverviewBucket(bucketStart, granularity);
            }
        }
        return bucketStart.toString();
    }

    private List<Long> resolveAreaScooterIds(Integer areaId) {
        if (areaId == null) {
            return null;
        }
        Area area = areaMapper.getById(areaId.longValue());
        if (area == null) {
            throw new BaseException("area not found");
        }
        List<List<Double>> polygon = parsePolygon(area.getPolygon());
        if (polygon.size() < 3) {
            return new ArrayList<>();
        }

        return findScootersInPolygon(scooterMapper.listLocated(), polygon);
    }

    private List<Long> findScootersInPolygon(List<Scooter> scooters, List<List<Double>> polygon) {
        List<Long> scooterIds = new ArrayList<>();
        if (scooters == null) {
            return scooterIds;
        }
        for (Scooter scooter : scooters) {
            if (scooter.getLongitude() == null || scooter.getLatitude() == null) {
                continue;
            }
            double longitude = scooter.getLongitude().doubleValue();
            double latitude = scooter.getLatitude().doubleValue();
            if (isPointInPolygon(latitude, longitude, polygon)) {
                scooterIds.add(scooter.getId());
            }
        }
        return scooterIds;
    }

    private List<List<Double>> parsePolygon(String polygon) {
        if (polygon == null || polygon.isBlank()) {
            return new ArrayList<>();
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(polygon, new TypeReference<List<List<Double>>>() {});
        } catch (Exception e) {
            throw new BaseException("invalid area polygon");
        }
    }

    private boolean isPointInPolygon(double latitude, double longitude, List<List<Double>> polygon) {
        boolean inside = false;
        int pointCount = polygon.size();
        for (int i = 0, j = pointCount - 1; i < pointCount; j = i++) {
            List<Double> current = polygon.get(i);
            List<Double> previous = polygon.get(j);
            if (current == null || previous == null || current.size() < 2 || previous.size() < 2) {
                return false;
            }

            double currentLatitude = current.get(0);
            double currentLongitude = current.get(1);
            double previousLatitude = previous.get(0);
            double previousLongitude = previous.get(1);

            boolean intersects = ((currentLatitude > latitude) != (previousLatitude > latitude))
                    && (longitude < (previousLongitude - currentLongitude) * (latitude - currentLatitude)
                    / (previousLatitude - currentLatitude) + currentLongitude);
            if (intersects) {
                inside = !inside;
            }
        }
        return inside;
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
                .createTime(area.getCreateTime())
                .dispatchers(buildZoneDetailDispatchers(area.getId()))
                .vehicleCount(countScootersInArea(area.getPolygon()))
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
        return EditZoneVO.builder()
                .zone(zoneInfo)
                .dispatchers(buildEditZoneDispatchers(updatedArea.getId()))
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
        int pageSize = dispatcherListDTO.getPagesize() != null ? dispatcherListDTO.getPagesize() : 10;
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
                .pagesize(pageSize)
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
        boolean hasDispatcherBaseUpdate = updateDispatcher.getName() != null
                || updateDispatcher.getPassword() != null
                || updateDispatcher.getEmail() != null;

        // 更新数据库
        int rows = hasDispatcherBaseUpdate ? dispatcherMapper.updateDispatcher(updateDispatcher) : 1;
        if (rows == 0) {
            throw new BaseException("edit dispatcher failed");
        }

        // 获取更新后的调度员信息
        if (editDispatcherDTO.isAreaIdPresent()) {
            rows = dispatcherMapper.updateAreaId(editDispatcherDTO.getId(), editDispatcherDTO.getAreaId());
            if (rows == 0) {
                throw new BaseException("edit dispatcher area failed");
            }
        }

        if (!hasDispatcherBaseUpdate && !editDispatcherDTO.isAreaIdPresent()) {
            throw new BaseException("no fields to update");
        }

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

    @Override
    public void deleteDispatcher(DeleteDispatcherDTO deleteDispatcherDTO) {
        if (deleteDispatcherDTO.getDispatcherId() == null) {
            throw new BaseException("dispatcherId is required");
        }
        Dispatcher dispatcher = dispatcherMapper.getById(deleteDispatcherDTO.getDispatcherId());
        if (dispatcher == null) {
            throw new BaseException("dispatcher not found");
        }
        if (deleteDispatcherDTO.getName() != null && !deleteDispatcherDTO.getName().isEmpty()
                && !deleteDispatcherDTO.getName().equals(dispatcher.getName())) {
            throw new BaseException("dispatcher name does not match");
        }
        if (deleteDispatcherDTO.getEmail() != null && !deleteDispatcherDTO.getEmail().isEmpty()
                && !deleteDispatcherDTO.getEmail().equals(dispatcher.getEmail())) {
            throw new BaseException("dispatcher email does not match");
        }
        if (dispatcherMapper.deleteById(deleteDispatcherDTO.getDispatcherId()) == 0) {
            throw new BaseException("delete dispatcher failed");
        }
    }

    @Override
    public NoParkingZoneListVO getNoParkingZoneList(NoParkingZoneListDTO listDTO) {
        int page = resolvePage(listDTO.getPage());
        int pageSize = resolvePageSize(listDTO.getPagesize());
        int offset = (page - 1) * pageSize;

        List<NoParkingArea> areas = noParkingAreaMapper.listForAdmin(offset, pageSize, listDTO.getKeyword());
        Integer total = noParkingAreaMapper.countForAdmin(listDTO.getKeyword());
        List<NoParkingZoneListVO.NoParkingZoneItem> items = new ArrayList<>();
        if (areas != null) {
            for (NoParkingArea area : areas) {
                items.add(NoParkingZoneListVO.NoParkingZoneItem.builder()
                        .id(area.getId())
                        .name(area.getName())
                        .polygon(area.getPolygon())
                        .status(area.getStatus())
                        .createTime(area.getCreateTime())
                        .build());
            }
        }

        return NoParkingZoneListVO.builder()
                .areaList(items)
                .page(page)
                .pagesize(pageSize)
                .total(total != null ? total : 0)
                .vehicleCount(countScootersInNoParkingAreas(areas))
                .build();
    }

    @Override
    public NoParkingZoneVO addNoParkingZone(AddNoParkingZoneDTO addDTO) {
        if (addDTO.getName() == null || addDTO.getName().isEmpty()) {
            throw new BaseException("name is required");
        }
        NoParkingArea area = new NoParkingArea();
        area.setName(addDTO.getName());
        area.setPolygon(addDTO.getPolygon());
        area.setStatus(addDTO.getStatus() != null ? addDTO.getStatus() : 1);
        area.setCreateTime(LocalDateTime.now());
        if (noParkingAreaMapper.insert(area) == 0) {
            throw new BaseException("add no parking zone failed");
        }
        return buildNoParkingZoneVO(area, countPolygonPoints(area.getPolygon()), currentAdminName());
    }

    @Override
    public NoParkingZoneVO editNoParkingZone(EditNoParkingZoneDTO editDTO) {
        if (editDTO.getId() == null) {
            throw new BaseException("id is required");
        }
        NoParkingArea existing = noParkingAreaMapper.getById(editDTO.getId());
        if (existing == null) {
            throw new BaseException("no parking zone not found");
        }
        NoParkingArea update = new NoParkingArea();
        update.setId(editDTO.getId());
        update.setName(editDTO.getName());
        update.setPolygon(editDTO.getPolygon());
        update.setStatus(editDTO.getStatus());
        if (noParkingAreaMapper.update(update) == 0) {
            throw new BaseException("edit no parking zone failed");
        }
        NoParkingArea updated = noParkingAreaMapper.getById(editDTO.getId());
        return buildNoParkingZoneVO(updated, countPolygonPoints(updated.getPolygon()), null);
    }

    @Override
    public void deleteNoParkingZone(DeleteNoParkingZoneDTO deleteDTO) {
        if (deleteDTO.getId() == null) {
            throw new BaseException("id is required");
        }
        NoParkingArea existing = noParkingAreaMapper.getById(deleteDTO.getId());
        if (existing == null) {
            throw new BaseException("no parking zone not found");
        }
        if (deleteDTO.getName() != null && !deleteDTO.getName().isEmpty()
                && !deleteDTO.getName().equals(existing.getName())) {
            throw new BaseException("no parking zone name does not match");
        }
        if (noParkingAreaMapper.deleteById(deleteDTO.getId()) == 0) {
            throw new BaseException("delete no parking zone failed");
        }
    }

    @Override
    public ParkingPointListVO getParkingPointList(ParkingPointListDTO listDTO) {
        int page = resolvePage(listDTO.getPage());
        int pageSize = resolvePageSize(listDTO.getPagesize());
        int offset = (page - 1) * pageSize;

        List<ParkingPoint> points = parkingPointMapper.listForAdmin(offset, pageSize, listDTO.getKeyword());
        Integer total = parkingPointMapper.countForAdmin(listDTO.getKeyword());
        List<ParkingPointListVO.ParkingPointItem> items = new ArrayList<>();
        if (points != null) {
            for (ParkingPoint point : points) {
                items.add(ParkingPointListVO.ParkingPointItem.builder()
                        .id(point.getId())
                        .name(point.getName())
                        .latitude(point.getLatitude())
                        .longitude(point.getLongitude())
                        .status(point.getStatus())
                        .createTime(point.getCreateTime())
                        .build());
            }
        }
        return ParkingPointListVO.builder()
                .areaList(items)
                .page(page)
                .pagesize(pageSize)
                .total(total != null ? total : 0)
                .build();
    }

    @Override
    public ParkingPointVO addParkingPoint(AddParkingPointDTO addDTO) {
        if (addDTO.getName() == null || addDTO.getName().isEmpty()) {
            throw new BaseException("name is required");
        }
        if (addDTO.getLatitude() == null || addDTO.getLongitude() == null) {
            throw new BaseException("latitude and longitude are required");
        }
        ParkingPoint point = new ParkingPoint();
        point.setName(addDTO.getName());
        point.setLatitude(addDTO.getLatitude());
        point.setLongitude(addDTO.getLongitude());
        point.setStatus(addDTO.getStatus() != null ? addDTO.getStatus() : 1);
        point.setCreateTime(LocalDateTime.now());
        if (parkingPointMapper.insert(point) == 0) {
            throw new BaseException("add parking point failed");
        }
        return buildParkingPointVO(point);
    }

    @Override
    public ParkingPointVO editParkingPoint(EditParkingPointDTO editDTO) {
        if (editDTO.getId() == null) {
            throw new BaseException("id is required");
        }
        ParkingPoint existing = parkingPointMapper.getById(editDTO.getId());
        if (existing == null) {
            throw new BaseException("parking point not found");
        }
        ParkingPoint update = new ParkingPoint();
        update.setId(editDTO.getId());
        update.setName(editDTO.getName());
        update.setLatitude(editDTO.getLatitude());
        update.setLongitude(editDTO.getLongitude());
        update.setStatus(editDTO.getStatus());
        if (parkingPointMapper.update(update) == 0) {
            throw new BaseException("edit parking point failed");
        }
        return buildParkingPointVO(parkingPointMapper.getById(editDTO.getId()));
    }

    @Override
    public void deleteParkingPoint(DeleteParkingPointDTO deleteDTO) {
        if (deleteDTO.getId() == null) {
            throw new BaseException("id is required");
        }
        ParkingPoint existing = parkingPointMapper.getById(deleteDTO.getId());
        if (existing == null) {
            throw new BaseException("parking point not found");
        }
        if (deleteDTO.getName() != null && !deleteDTO.getName().isEmpty()
                && !deleteDTO.getName().equals(existing.getName())) {
            throw new BaseException("parking point name does not match");
        }
        if (parkingPointMapper.deleteById(deleteDTO.getId()) == 0) {
            throw new BaseException("delete parking point failed");
        }
    }

    @Override
    public AdminScooterListVO getScooterList(Integer areaId) {
        List<Scooter> scooters;
        if (areaId == null) {
            scooters = scooterMapper.listAll();
        } else {
            List<Long> scooterIds = resolveAreaScooterIds(areaId);
            scooters = new ArrayList<>();
            if (scooterIds != null && !scooterIds.isEmpty()) {
                Map<Long, Scooter> scooterById = new HashMap<>();
                for (Scooter scooter : scooterMapper.listLocated()) {
                    scooterById.put(scooter.getId(), scooter);
                }
                for (Long scooterId : scooterIds) {
                    Scooter scooter = scooterById.get(scooterId);
                    if (scooter != null) {
                        scooters.add(scooter);
                    }
                }
            }
        }

        List<AdminScooterListVO.ScooterItem> items = new ArrayList<>();
        if (scooters != null) {
            for (Scooter scooter : scooters) {
                items.add(AdminScooterListVO.ScooterItem.builder()
                        .id(scooter.getId())
                        .code(scooter.getCode())
                        .rideStatus(scooter.getRideStatus())
                        .faultStatus(scooter.getFaultStatus())
                        .battery(scooter.getBattery())
                        .latitude(scooter.getLatitude())
                        .longitude(scooter.getLongitude())
                        .createTime(scooter.getCreateTime())
                        .build());
            }
        }
        return AdminScooterListVO.builder()
                .areaList(items)
                .areaId(areaId)
                .scooterCount(String.valueOf(items.size()))
                .build();
    }

    private int resolvePage(Integer page) {
        return page != null && page > 0 ? page : 1;
    }

    private int resolvePageSize(Integer pageSize) {
        return pageSize != null && pageSize > 0 ? pageSize : 10;
    }

    private Integer countScootersInNoParkingAreas(List<NoParkingArea> areas) {
        if (areas == null || areas.isEmpty()) {
            return 0;
        }
        List<Scooter> scooters = scooterMapper.listLocated();
        Set<Long> scooterIds = new HashSet<>();
        for (NoParkingArea area : areas) {
            try {
                List<List<Double>> polygon = parsePolygon(area.getPolygon());
                if (polygon.size() >= 3) {
                    scooterIds.addAll(findScootersInPolygon(scooters, polygon));
                }
            } catch (BaseException ignored) {
            }
        }
        return scooterIds.size();
    }

    private NoParkingZoneVO buildNoParkingZoneVO(NoParkingArea area, Integer polygonPointCount, String createdBy) {
        return NoParkingZoneVO.builder()
                .id(area.getId())
                .name(area.getName())
                .polygon(area.getPolygon())
                .status(area.getStatus())
                .createTime(area.getCreateTime())
                .polygonPointCount(polygonPointCount)
                .createdBy(createdBy)
                .build();
    }

    private ParkingPointVO buildParkingPointVO(ParkingPoint point) {
        return ParkingPointVO.builder()
                .id(point.getId())
                .name(point.getName())
                .latitude(point.getLatitude())
                .longitude(point.getLongitude())
                .status(point.getStatus())
                .createTime(point.getCreateTime())
                .build();
    }

    private String currentAdminName() {
        Long adminId = BaseContext.getCurrentId();
        return adminId != null ? adminId.toString() : "admin";
    }

    private String encrypt(String value) {
        return DigestUtils.md5DigestAsHex(value.getBytes(StandardCharsets.UTF_8));
    }
}
