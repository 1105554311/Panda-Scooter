<template>
  <view class="page">
    <view class="map-container">
      <map
        class="map"
        :latitude="latitude"
        :longitude="longitude"
        :scale="scale"
        :markers="markers"
        :polygons="polygons"
        :show-location="true"
        @markertap="handleMarkerTap"
      ></map>
    </view>

    <view class="result-panel">
      <view class="result-meta">
        <text class="result-text">共 {{ violatingScooters.length }} 辆禁停区车辆</text>
      </view>

      <scroll-view class="result-list" scroll-y>
        <view
          v-for="item in violatingScooters"
          :key="item.id"
          class="result-item ui-pressable"
          :class="{ active: item.id === selectedScooterId }"
          hover-class="ui-pressable-hover"
          hover-stay-time="70"
          @click="selectScooter(item)"
        >
          <view class="result-header">
            <text class="result-name">{{ item.code }}</text>
            <text class="result-distance">{{ formatDistance(item.distance) }}</text>
          </view>

          <view class="result-subline">
            <text class="result-area">{{ item.areaName }}</text>
          </view>

          <view class="result-details">
            <text class="detail-text">状态 {{ item.rideStatusText }}</text>
            <text class="detail-text">电量 {{ item.batteryText }}</text>
            <text class="detail-text" :class="{ 'fault-text': item.faultStatus === 1 }">{{ item.faultStatusText }}</text>
          </view>
        </view>

        <view v-if="!violatingScooters.length" class="empty-state">
          <text class="empty-text">当前没有位于禁停区的车辆</text>
        </view>
      </scroll-view>
    </view>
  </view>
</template>

<script>
import { getMapData } from '@/api/index'
import { isUnauthorizedError } from '@/utils/auth'

const DEFAULT_LOCATION = {
  latitude: 30.75953206821905,
  longitude: 103.98442619779992
}

const SCOOTER_ICON = '/static/scooter.svg'

export default {
  data() {
    return {
      latitude: DEFAULT_LOCATION.latitude,
      longitude: DEFAULT_LOCATION.longitude,
      originLatitude: DEFAULT_LOCATION.latitude,
      originLongitude: DEFAULT_LOCATION.longitude,
      scale: 17,
      selectedScooterId: null,
      selectedAreaId: null,
      violatingScooters: [],
      noParkingAreas: []
    }
  },
  computed: {
    markers() {
      return this.violatingScooters.map((item) => ({
        id: item.id,
        latitude: item.latitude,
        longitude: item.longitude,
        iconPath: SCOOTER_ICON,
        width: item.id === this.selectedScooterId ? 40 : 34,
        height: item.id === this.selectedScooterId ? 40 : 34,
        callout: {
          content: item.code,
          color: '#0b0e0d',
          fontSize: 12,
          borderRadius: 6,
          bgColor: '#ffffff',
          padding: 6,
          display: item.id === this.selectedScooterId ? 'ALWAYS' : 'BYCLICK'
        }
      }))
    },
    polygons() {
      return this.noParkingAreas.map((item) => ({
        id: item.id,
        points: item.points,
        fillColor: item.id === this.selectedAreaId ? '#FF4D4F52' : '#FF4D4F2E',
        strokeColor: item.id === this.selectedAreaId ? '#D9363E' : '#FF4D4F73',
        strokeWidth: item.id === this.selectedAreaId ? 3 : 2
      }))
    }
  },
  async onLoad() {
    if (!uni.getStorageSync('dispatcherToken')) {
      uni.redirectTo({
        url: '/pages/login/login?mode=login'
      })
      return
    }

    await this.loadVehicleLookup()
  },
  methods: {
    getLocation() {
      return new Promise((resolve, reject) => {
        uni.getLocation({
          type: 'gcj02',
          success: resolve,
          fail: reject
        })
      })
    },
    async loadVehicleLookup() {
      await this.loadCurrentLocation()
      await this.loadMapData()
    },
    async loadCurrentLocation() {
      try {
        const location = await this.getLocation()
        this.latitude = Number(location.latitude)
        this.longitude = Number(location.longitude)
        this.originLatitude = Number(location.latitude)
        this.originLongitude = Number(location.longitude)
      } catch (error) {
        uni.showToast({
          title: '定位失败，将按默认位置查询',
          icon: 'none'
        })
      }
    },
    async loadMapData() {
      try {
        const res = await getMapData({
          latitude: this.latitude,
          longitude: this.longitude,
          scale: this.normalizeMapScale(this.scale)
        })
        const data = res.data || {}
        const noParkingAreas = this.normalizeNoParkingAreas(data.noParkingAreas || [])
        const scooters = this.normalizeScooters(data.scooters || [])
        const violatingScooters = this.filterViolatingScooters(scooters, noParkingAreas)
        const activeAreaIds = new Set(violatingScooters.map((item) => item.areaId))

        this.noParkingAreas = noParkingAreas.filter((item) => activeAreaIds.has(item.id))
        this.violatingScooters = violatingScooters

        if (violatingScooters.length) {
          this.selectScooter(violatingScooters[0], false)
          return
        }

        this.selectedScooterId = null
        this.selectedAreaId = null
      } catch (error) {
        this.violatingScooters = []
        this.noParkingAreas = []
        this.selectedScooterId = null
        this.selectedAreaId = null

        if (isUnauthorizedError(error)) {
          uni.reLaunch({
            url: '/pages/login/login?mode=login'
          })
          return
        }

        if (!error || !error.handled) {
          uni.showToast({
            title: '加载车辆查询失败，请稍后重试',
            icon: 'none'
          })
        }
      }
    },
    normalizeNoParkingAreas(list) {
      return list
        .map((item, index) => {
          const points = this.parsePolygon(item.polygon)
          if (!points.length) {
            return null
          }

          return {
            id: Number(item.id) || index + 1,
            name: item.name || `禁停区 ${index + 1}`,
            points
          }
        })
        .filter(Boolean)
    },
    normalizeScooters(list) {
      return list
        .map((item, index) => {
          const point = this.normalizePoint(item)
          if (!point) {
            return null
          }

          const rideStatus = Number(item.rideStatus ?? item.ride_status)
          const faultStatus = Number(item.faultStatus ?? item.fault_status)
          const batteryValue = this.extractBatteryValue(item.battery)

          return {
            id: Number(item.id) || index + 1,
            code: item.code || `车辆 ${index + 1}`,
            latitude: point.latitude,
            longitude: point.longitude,
            rideStatus: Number.isFinite(rideStatus) ? rideStatus : 0,
            faultStatus: Number.isFinite(faultStatus) ? faultStatus : 0,
            batteryValue,
            batteryText: Number.isFinite(batteryValue) ? `${batteryValue}%` : '--'
          }
        })
        .filter(Boolean)
    },
    filterViolatingScooters(scooters, noParkingAreas) {
      return scooters
        .map((item) => {
          const matchedArea = noParkingAreas.find((area) => this.isPointInPolygon(item, area.points))
          if (!matchedArea) {
            return null
          }

          return {
            ...item,
            areaId: matchedArea.id,
            areaName: matchedArea.name,
            distance: this.calculateDistance(
              this.originLatitude,
              this.originLongitude,
              item.latitude,
              item.longitude
            ),
            rideStatusText: this.mapRideStatus(item.rideStatus),
            faultStatusText: item.faultStatus === 1 ? '故障' : '正常'
          }
        })
        .filter(Boolean)
        .sort((first, second) => first.distance - second.distance)
    },
    handleMarkerTap(event) {
      const current = this.violatingScooters.find((item) => item.id === event.detail.markerId)
      if (current) {
        this.selectScooter(current)
      }
    },
    selectScooter(item, adjustScale = true) {
      this.selectedScooterId = item.id
      this.selectedAreaId = item.areaId
      this.latitude = item.latitude
      this.longitude = item.longitude

      if (adjustScale) {
        this.scale = this.normalizeMapScale(18)
      }
    },
    normalizeMapScale(scale) {
      const roundedScale = Math.round(Number(scale))
      if (!Number.isFinite(roundedScale)) {
        return 17
      }

      return Math.min(20, Math.max(3, roundedScale))
    },
    normalizePoint(point) {
      if (!point) {
        return null
      }

      if (Array.isArray(point) && point.length >= 2) {
        const first = Number(point[0])
        const second = Number(point[1])
        if (!Number.isFinite(first) || !Number.isFinite(second)) {
          return null
        }

        if (Math.abs(first) <= 90 && Math.abs(second) <= 180) {
          return { latitude: first, longitude: second }
        }

        return { latitude: second, longitude: first }
      }

      const latitude = Number(point.latitude)
      const longitude = Number(point.longitude)
      if (!Number.isFinite(latitude) || !Number.isFinite(longitude)) {
        return null
      }

      return { latitude, longitude }
    },
    parsePolygon(polygon) {
      if (!polygon) {
        return []
      }

      if (Array.isArray(polygon)) {
        return polygon.map((item) => this.normalizePoint(item)).filter(Boolean)
      }

      if (typeof polygon === 'string') {
        try {
          const parsed = JSON.parse(polygon)
          return Array.isArray(parsed)
            ? parsed.map((item) => this.normalizePoint(item)).filter(Boolean)
            : []
        } catch (error) {
          return polygon
            .split(';')
            .map((segment) => segment.trim())
            .filter(Boolean)
            .map((segment) => {
              const values = segment
                .replace(/^\[|\]$/g, '')
                .split(',')
                .map((item) => Number(item.trim()))
              return this.normalizePoint(values)
            })
            .filter(Boolean)
        }
      }

      return []
    },
    isPointInPolygon(point, polygon) {
      if (!polygon.length) {
        return false
      }

      const x = Number(point.longitude)
      const y = Number(point.latitude)
      let isInside = false

      for (let index = 0, previousIndex = polygon.length - 1; index < polygon.length; previousIndex = index, index += 1) {
        const current = polygon[index]
        const previous = polygon[previousIndex]
        const currentX = Number(current.longitude)
        const currentY = Number(current.latitude)
        const previousX = Number(previous.longitude)
        const previousY = Number(previous.latitude)

        const intersect = ((currentY > y) !== (previousY > y))
          && (x < ((previousX - currentX) * (y - currentY)) / ((previousY - currentY) || Number.EPSILON) + currentX)

        if (intersect) {
          isInside = !isInside
        }
      }

      return isInside
    },
    calculateDistance(fromLatitude, fromLongitude, toLatitude, toLongitude) {
      const toRadians = (degree) => degree * (Math.PI / 180)
      const earthRadius = 6371000
      const deltaLatitude = toRadians(toLatitude - fromLatitude)
      const deltaLongitude = toRadians(toLongitude - fromLongitude)
      const startLatitude = toRadians(fromLatitude)
      const endLatitude = toRadians(toLatitude)
      const haversine = Math.sin(deltaLatitude / 2) ** 2
        + Math.cos(startLatitude) * Math.cos(endLatitude) * Math.sin(deltaLongitude / 2) ** 2
      const arc = 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine))
      return earthRadius * arc
    },
    extractBatteryValue(value) {
      const numericValue = Number(value)
      return Number.isFinite(numericValue) ? numericValue : null
    },
    formatDistance(distance) {
      const meters = Number(distance || 0)
      if (meters < 1000) {
        return `直线距离 ${Math.round(meters)} m`
      }

      return `直线距离 ${(meters / 1000).toFixed(2)} km`
    },
    mapRideStatus(status) {
      const statusMap = {
        0: '空闲',
        1: '使用中',
        2: '维修中',
        3: '调度中'
      }

      return statusMap[Number(status)] || '--'
    }
  }
}
</script>

<style>
.page {
  min-height: 100vh;
  background-color: #fafaf8;
}

.map-container {
  width: 100%;
  height: 520rpx;
  background-color: #ffffff;
  border-bottom: 1rpx solid #e5e5e2;
}

.map {
  width: 100%;
  height: 100%;
}

.result-panel {
  padding: 32rpx;
}

.result-meta {
  margin-bottom: 20rpx;
}

.result-text {
  font-size: 24rpx;
  color: #737373;
}

.result-list {
  height: calc(100vh - 620rpx);
}

.result-item {
  margin-bottom: 16rpx;
  padding: 28rpx 24rpx;
  background-color: #ffffff;
  border: 1rpx solid #e5e5e2;
}

.result-item.active {
  border-color: #0b0e0d;
}

.result-header,
.result-subline,
.result-details {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.result-header {
  margin-bottom: 12rpx;
}

.result-subline {
  margin-bottom: 12rpx;
}

.result-name {
  font-size: 28rpx;
  color: #0b0e0d;
}

.result-distance,
.result-area,
.detail-text {
  font-size: 22rpx;
  color: #737373;
}

.result-area {
  color: #a67c00;
}

.fault-text {
  color: #c62828;
}

.empty-state {
  padding: 96rpx 0;
  text-align: center;
}

.empty-text {
  font-size: 24rpx;
  color: #737373;
}
</style>
