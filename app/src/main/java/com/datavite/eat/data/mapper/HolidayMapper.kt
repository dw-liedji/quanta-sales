package com.datavite.eat.data.mapper

import com.datavite.eat.data.local.SyncType
import com.datavite.eat.data.local.model.LocalHoliday
import com.datavite.eat.data.remote.model.RemoteHoliday
import com.datavite.eat.domain.model.DomainHoliday

class HolidayMapper {

    // Convert RemoteHoliday to DomainHoliday
    fun mapRemoteToDomain(remoteHoliday: RemoteHoliday): DomainHoliday {
        return DomainHoliday(
            id = remoteHoliday.id,
            created = remoteHoliday.created,
            modified = remoteHoliday.modified,
            orgId = remoteHoliday.orgId,
            orgSlug = remoteHoliday.orgSlug,
            name = remoteHoliday.name,
            date = remoteHoliday.date,
            type = remoteHoliday.type
        )
    }

    // Convert LocalHoliday to DomainHoliday
    fun mapLocalToDomain(localHoliday: LocalHoliday): DomainHoliday {
        return DomainHoliday(
            id = localHoliday.id,
            created = localHoliday.created,
            modified = localHoliday.modified,
            orgId = localHoliday.orgId,
            orgSlug = localHoliday.orgSlug,
            name = localHoliday.name,
            date = localHoliday.date,
            type = localHoliday.type
        )
    }

    // Convert DomainHoliday to RemoteHoliday
    fun mapDomainToRemote(domainHoliday: DomainHoliday): RemoteHoliday {
        return RemoteHoliday(
            id = domainHoliday.id,
            created = domainHoliday.created,
            modified = domainHoliday.modified,
            orgId = domainHoliday.orgId,
            orgSlug = domainHoliday.orgSlug,
            name = domainHoliday.name,
            date = domainHoliday.date,
            type = domainHoliday.type
        )
    }

    // Convert DomainHoliday to LocalHoliday
    fun mapDomainToLocal(domainHoliday: DomainHoliday, syncType: SyncType): LocalHoliday {
        return LocalHoliday(
            id = domainHoliday.id,
            created = domainHoliday.created,
            modified = domainHoliday.modified,
            orgId = domainHoliday.orgId,
            orgSlug = domainHoliday.orgSlug,
            name = domainHoliday.name,
            date = domainHoliday.date,
            type = domainHoliday.type,
            syncType= syncType
        )
    }
}
