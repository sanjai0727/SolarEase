"""Add indexes on users.username and billing_cycles composite user_active

Revision ID: f782a19b0d24
Revises: e45d6c2e1dc3
Create Date: 2026-10-28 17:40:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = 'f782a19b0d24'
down_revision: Union[str, Sequence[str], None] = 'e45d6c2e1dc3'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # 1. Index on users.username for fast profile and auth lookups
    op.create_index(op.f('ix_users_username'), 'users', ['username'], unique=True)
    # 2. Composite index on billing_cycles (user_id, is_active) for active baseline scans
    op.create_index('ix_billing_cycles_user_is_active', 'billing_cycles', ['user_id', 'is_active'], unique=False)


def downgrade() -> None:
    op.drop_index('ix_billing_cycles_user_is_active', table_name='billing_cycles')
    op.drop_index(op.f('ix_users_username'), table_name='users')
