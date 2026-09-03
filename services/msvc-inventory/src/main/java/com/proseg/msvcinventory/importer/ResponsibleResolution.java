package com.proseg.msvcinventory.importer;

import com.proseg.msvcinventory.entity.Employee;

public sealed interface ResponsibleResolution {

    record None() implements ResponsibleResolution {
    }

    record Use(Employee employee) implements ResponsibleResolution {
    }

    record Create(String name, String identification) implements ResponsibleResolution {
    }

    record AssignIdentification(Employee employee, String identification) implements ResponsibleResolution {
    }
}
