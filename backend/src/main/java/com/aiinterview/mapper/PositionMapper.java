package com.aiinterview.mapper;

import com.aiinterview.entity.Position;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PositionMapper extends BaseMapper<Position> {

    @Select("SELECT id, code, is_deleted FROM t_position WHERE code = #{code} LIMIT 1")
    Position selectAnyByCode(@Param("code") String code);

    @Update("""
            <script>
            UPDATE t_position
            SET name = #{p.name},
                description = #{p.description},
                <choose>
                    <when test="p.techStack != null">
                        tech_stack = #{p.techStack,typeHandler=com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler},
                    </when>
                    <otherwise>
                        tech_stack = NULL,
                    </otherwise>
                </choose>
                icon_url = COALESCE(#{p.iconUrl}, ''),
                sort_order = COALESCE(#{p.sortOrder}, 0),
                is_active = 1,
                is_deleted = 0,
                updated_at = NOW()
            WHERE id = #{p.id}
            </script>
            """)
    int restoreDeleted(@Param("p") Position position);
}
