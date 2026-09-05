package com.electricaldesignengineer.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,

    val clientName: String,

    val projectLocation: String,

    val engineerName: String,

    val createdDate: String
)
