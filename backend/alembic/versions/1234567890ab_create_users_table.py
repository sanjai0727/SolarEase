"""create_users_table

Revision ID: 1234567890ab
Revises: 
Create Date: 2026-08-03 23:45:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = '1234567890ab'
down_revision: Union[str, None] = None
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table('users',
    sa.Column('id', sa.UUID(as_uuid=True), nullable=False),
    sa.Column('mobile_number', sa.String(length=10), nullable=False),
    sa.Column('full_name', sa.String(length=255), nullable=True),
    sa.Column('is_verified', sa.Boolean(), nullable=False),
    sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_users_mobile_number'), 'users', ['mobile_number'], unique=True)


def downgrade() -> None:
    op.drop_index(op.f('ix_users_mobile_number'), table_name='users')
    op.drop_table('users')
