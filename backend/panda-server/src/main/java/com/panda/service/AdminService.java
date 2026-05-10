package com.panda.service;

import com.panda.dto.*;
import com.panda.vo.*;

import java.util.List;

public interface AdminService {

    AdminLoginVO login(AdminLoginDTO loginDTO);

    void logout();

    DataOverviewVO getOverview(DataOverviewDTO overviewDTO);

    LiveDataVO getLiveData(LiveDataDTO liveDataDTO);

    PricingRuleVO getPricingRules();

    void editRules(PricingRuleEditDTO editDTO);

    PackageListVO getPackageList(PackageListDTO packageListDTO);

    void addPackage(AddPackageDTO addPackageDTO);

    void editPackage(EditPackageDTO editPackageDTO);

    void deletePackage(DeletePackageDTO deletePackageDTO);

    ZoneListVO getZoneList(ZoneListDTO zoneListDTO);

    AddZoneVO addZone(AddZoneDTO addZoneDTO);

    ZoneDetailVO getZoneDetail(ZoneDetailDTO zoneDetailDTO);

    EditZoneVO editZone(EditZoneDTO editZoneDTO);

    void deleteZone(DeleteZoneDTO deleteZoneDTO);

    DispatcherListVO getDispatcherList(DispatcherListDTO dispatcherListDTO);

    AddDispatcherVO addDispatcher(AddDispatcherDTO addDispatcherDTO);

    EditDispatcherVO editDispatcher(EditDispatcherDTO editDispatcherDTO);

    void deleteDispatcher(DeleteDispatcherDTO deleteDispatcherDTO);

    NoParkingZoneListVO getNoParkingZoneList(NoParkingZoneListDTO listDTO);

    NoParkingZoneVO addNoParkingZone(AddNoParkingZoneDTO addDTO);

    NoParkingZoneVO editNoParkingZone(EditNoParkingZoneDTO editDTO);

    void deleteNoParkingZone(DeleteNoParkingZoneDTO deleteDTO);

    ParkingPointListVO getParkingPointList(ParkingPointListDTO listDTO);

    ParkingPointVO addParkingPoint(AddParkingPointDTO addDTO);

    ParkingPointVO editParkingPoint(EditParkingPointDTO editDTO);

    void deleteParkingPoint(DeleteParkingPointDTO deleteDTO);

    AdminScooterListVO getScooterList(Integer areaId);
}
