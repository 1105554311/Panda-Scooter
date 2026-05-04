package com.panda.service.impl;

import com.panda.context.BaseContext;
import com.panda.dto.*;
import com.panda.entity.Admin;
import com.panda.entity.PricingRule;
import com.panda.entity.SubscriptionPackage;
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

    private String encrypt(String value) {
        return DigestUtils.md5DigestAsHex(value.getBytes(StandardCharsets.UTF_8));
    }
}